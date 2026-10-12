/**
 * Generates feature list/detail/form pages for FlyPro frontend.
 * Run: node scripts/generate-features.mjs
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const src = path.join(root, "src");

function write(rel, content) {
  const full = path.join(src, rel);
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, content, "utf8");
}

write(
  "features/auth/LoginPage.tsx",
  `import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useLocation } from "react-router-dom";
import { z } from "zod";
import toast from "react-hot-toast";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { FieldError, Input, Label } from "@/components/ui/FormControls";
import { useAuth } from "@/contexts/AuthContext";

const schema = z.object({
  username: z.string().min(1, "Identifiant requis"),
  password: z.string().min(1, "Mot de passe requis"),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [submitting, setSubmitting] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = handleSubmit(async (values) => {
    setSubmitting(true);
    try {
      await login(values.username, values.password);
      toast.success("Connexion réussie");
      const redirect = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? "/dashboard";
      navigate(redirect, { replace: true });
    } catch {
      toast.error("Identifiants invalides");
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <Card>
      <CardBody>
        <h2 className="font-display text-2xl">Connexion</h2>
        <p className="mt-1 text-sm text-[var(--color-ink-500)]">Accédez à la console FlyPro Analytics</p>
        <form className="mt-6 space-y-4" onSubmit={onSubmit}>
          <div>
            <Label htmlFor="username">Nom d'utilisateur</Label>
            <Input id="username" autoComplete="username" {...register("username")} />
            <FieldError message={errors.username?.message} />
          </div>
          <div>
            <Label htmlFor="password">Mot de passe</Label>
            <Input id="password" type="password" autoComplete="current-password" {...register("password")} />
            <FieldError message={errors.password?.message} />
          </div>
          <Button className="w-full" disabled={submitting}>
            {submitting ? "Connexion…" : "Se connecter"}
          </Button>
        </form>
        <p className="mt-4 text-xs text-[var(--color-ink-500)]">Compte démo : admin / Admin@123</p>
      </CardBody>
    </Card>
  );
}
`,
);

console.log("Login generated");
