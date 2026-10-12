import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useLocation } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { useState } from "react";
import toast from "react-hot-toast";
import { KeyRound, Lock, LogIn, ScanFace, ShieldCheck, UserRound } from "lucide-react";
import { api, unwrap } from "@/api/client";
import { faceApi } from "@/api/services";
import { tokenStore } from "@/api/tokenStore";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { FieldError, Input, Label } from "@/components/ui/FormControls";
import { WebcamCapture } from "@/components/shared/WebcamCapture";
import { APP_BRAND, ROLES } from "@/constants/app";
import type { ApiResponse, LoginResponse, RoleName } from "@/types/api";

const schema = z.object({
  username: z.string().min(1, "Identifiant requis"),
  password: z.string().min(1, "Mot de passe requis"),
});

type FormValues = z.infer<typeof schema>;

function homeForRole(role: RoleName) {
  return role === ROLES.EMPLOYEE ? "/employee" : "/dashboard";
}

function faceErrorMessage(err: unknown): string {
  const ax = err as { response?: { data?: { code?: string; message?: string } } };
  const code = ax.response?.data?.code;
  if (code === "FACE_NOT_ENROLLED") {
    return "Aucun visage enrôlé sur ce compte. Demandez à un administrateur d'activer la vérification faciale sur votre fiche.";
  }
  if (code === "NO_FACE_DETECTED") {
    return "Aucun visage détecté. Recentrez votre visage face à la caméra et réessayez.";
  }
  return ax.response?.data?.message ?? "Vérification faciale impossible";
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const [submitting, setSubmitting] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [step, setStep] = useState<"credentials" | "face">("credentials");
  const [pendingAuth, setPendingAuth] = useState<LoginResponse | null>(null);
  const [faceImage, setFaceImage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  function goTo(role: RoleName) {
    const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname;
    const fallback = homeForRole(role);
    navigate(from && !from.startsWith("/login") ? from : fallback, { replace: true });
  }

  function activateSession(payload: LoginResponse) {
    tokenStore.set(payload.accessToken, payload.refreshToken);
    queryClient.setQueryData(["auth", "me"], payload.user);
  }

  const onSubmit = handleSubmit(async (values) => {
    setSubmitting(true);
    try {
      const { data } = await api.post<ApiResponse<LoginResponse>>("/api/v1/auth/login", values);
      const payload = unwrap(data);
      if (payload.user.role === ROLES.EMPLOYEE) {
        // Tokens are intentionally NOT stored yet: the session only becomes
        // active once the face verification step below succeeds.
        setPendingAuth(payload);
        setStep("face");
      } else {
        activateSession(payload);
        toast.success("Connexion réussie");
        goTo(payload.user.role);
      }
    } catch (err) {
      const ax = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
      const status = ax.response?.status;
      const apiMessage = ax.response?.data?.message;
      if (status === 403) {
        toast.error(apiMessage ?? "Accès refusé (403).");
      } else if (status === 429) {
        toast.error(apiMessage ?? "Trop de tentatives. Attendez quelques minutes.");
      } else if (status === 401) {
        toast.error(apiMessage ?? "Identifiant ou mot de passe incorrect.");
      } else {
        toast.error(apiMessage ?? ax.message ?? "Connexion impossible");
      }
    } finally {
      setSubmitting(false);
    }
  });

  function resetToCredentials() {
    setStep("credentials");
    setPendingAuth(null);
    setFaceImage(null);
  }

  async function handleVerifyFace() {
    if (!pendingAuth || !faceImage) return;
    const employeeId = pendingAuth.user.employeeId;
    if (!employeeId) {
      toast.error("Ce compte n'est pas rattaché à une fiche employé.");
      resetToCredentials();
      return;
    }
    setVerifying(true);
    try {
      const result = await faceApi.verify(employeeId, faceImage);
      if (result.matched) {
        activateSession(pendingAuth);
        toast.success("Identité confirmée");
        goTo(pendingAuth.user.role);
      } else {
        toast.error("Le visage capturé ne correspond pas à ce compte.");
        setFaceImage(null);
      }
    } catch (err) {
      toast.error(faceErrorMessage(err));
      setFaceImage(null);
    } finally {
      setVerifying(false);
    }
  }

  if (step === "face" && pendingAuth) {
    return (
      <Card className="border-0 shadow-[0_24px_60px_-24px_rgba(28,24,23,0.5)] ring-1 ring-white/20 lg:bg-transparent lg:shadow-none lg:ring-0">
        <CardBody className="p-8 text-center lg:p-0">
          <span className="inline-flex items-center gap-2 rounded-full bg-brand-50 px-3 py-1 text-[11px] font-semibold tracking-[0.18em] text-brand-700 uppercase">
            <ScanFace className="size-3.5" />
            Vérification faciale requise
          </span>
          <h2 className="ram-wordmark mt-4 text-2xl text-brand-600 lg:text-3xl lg:text-[var(--color-ink-950)]">
            Bonjour {pendingAuth.user.username}
          </h2>
          <p className="mt-2 text-sm text-[var(--color-ink-500)]">
            Votre identifiant et votre mot de passe sont corrects. Pour finaliser la connexion,
            confirmez votre identité par reconnaissance faciale.
          </p>

          <div className="mt-6 flex flex-col items-center">
            <WebcamCapture onCapture={setFaceImage} />
          </div>

          <div className="mt-6 flex flex-col gap-2">
            <Button
              className="w-full bg-gradient-to-r from-brand-700 via-brand-600 to-brand-700 shadow-lg shadow-brand-600/25"
              disabled={!faceImage || verifying}
              onClick={() => void handleVerifyFace()}
            >
              <ShieldCheck className="size-4" />
              {verifying ? "Vérification…" : "Vérifier mon identité"}
            </Button>
            <Button type="button" variant="ghost" onClick={resetToCredentials} disabled={verifying}>
              Annuler et revenir à la connexion
            </Button>
          </div>

          <p className="mt-6 text-xs text-[var(--color-ink-400)]">
            Démonstration académique : le traitement du visage reste local au service IA de la plateforme.
          </p>
        </CardBody>
      </Card>
    );
  }

  return (
    <Card className="border-0 shadow-[0_24px_60px_-24px_rgba(28,24,23,0.5)] ring-1 ring-white/20 lg:bg-transparent lg:shadow-none lg:ring-0">
      <CardBody className="p-8 lg:p-0">
        <div className="mb-8 flex justify-center lg:hidden">
          <img
            src="/brand/royal-air-maroc-logo.png"
            alt="Royal Air Maroc"
            className="h-16 w-auto object-contain sm:h-20"
            decoding="async"
          />
        </div>

        <span className="hidden items-center gap-2 rounded-full bg-brand-50 px-3 py-1 text-[11px] font-semibold tracking-[0.18em] text-brand-700 uppercase lg:inline-flex">
          <KeyRound className="size-3.5" />
          Accès sécurisé
        </span>

        <h2 className="ram-wordmark mt-4 text-center text-2xl text-brand-600 lg:mt-5 lg:text-left lg:text-3xl lg:text-[var(--color-ink-950)]">
          Connexion
        </h2>
        <p className="mt-2 text-center text-sm text-[var(--color-ink-500)] lg:text-left">
          Identifiez-vous pour accéder à votre espace {APP_BRAND}
        </p>
        <div
          className="mx-auto mt-4 h-1 w-14 rounded-full bg-gradient-to-r from-brand-600 to-[var(--color-gold)] lg:mx-0"
          aria-hidden
        />

        <form className="mt-8 space-y-5" onSubmit={onSubmit}>
          <div>
            <Label htmlFor="username">Identifiant</Label>
            <div className="relative">
              <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-[var(--color-ink-400)]" />
              <Input
                id="username"
                autoComplete="username"
                className="pl-10"
                placeholder="matricule ou identifiant"
                {...register("username")}
              />
            </div>
            <FieldError message={errors.username?.message} />
          </div>
          <div>
            <Label htmlFor="password">Mot de passe</Label>
            <div className="relative">
              <Lock className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-[var(--color-ink-400)]" />
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                className="pl-10"
                placeholder="••••••••"
                {...register("password")}
              />
            </div>
            <FieldError message={errors.password?.message} />
          </div>
          <Button
            className="w-full bg-gradient-to-r from-brand-700 via-brand-600 to-brand-700 shadow-lg shadow-brand-600/25"
            disabled={submitting}
          >
            <LogIn className="size-4" />
            {submitting ? "Connexion…" : "Se connecter"}
          </Button>
        </form>

        <p className="mt-8 text-center text-xs text-[var(--color-ink-400)] lg:text-left">
          Accès réservé au personnel autorisé {APP_BRAND}. Toute tentative non autorisée est journalisée.
        </p>
      </CardBody>
    </Card>
  );
}
