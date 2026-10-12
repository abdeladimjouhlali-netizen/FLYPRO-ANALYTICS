import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { ScanFace, ShieldCheck, ShieldX } from "lucide-react";
import { employeesApi, faceApi, type FaceVerifyResult } from "@/api/services";
import { WebcamCapture } from "@/components/shared/WebcamCapture";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { Label, Select } from "@/components/ui/FormControls";

function errorMessage(err: unknown): string {
  const ax = err as { response?: { data?: { code?: string; message?: string } } };
  const code = ax.response?.data?.code;
  if (code === "FACE_NOT_ENROLLED") {
    return "Aucun visage enrôlé pour cet employé. Enrôlez-le d'abord depuis sa fiche employé.";
  }
  if (code === "NO_FACE_DETECTED") {
    return "Aucun visage détecté dans l'image. Recentrez le visage et recapturez la photo.";
  }
  return ax.response?.data?.message ?? "Vérification impossible";
}

export function FaceVerificationPage() {
  const [employeeId, setEmployeeId] = useState(0);
  const [image, setImage] = useState<string | null>(null);

  const employees = useQuery({
    queryKey: ["employees", "for-face"],
    queryFn: () => employeesApi.list({ page: 0, size: 200, sort: "lastName,asc" }),
  });

  const verify = useMutation({
    mutationFn: () => faceApi.verify(employeeId, image as string),
    onError: (err) => toast.error(errorMessage(err)),
  });

  const result: FaceVerifyResult | undefined = verify.data;
  const selectedEmployee = employees.data?.content.find((e) => e.id === employeeId);

  return (
    <div>
      <PageHeader
        title="Vérification faciale"
        description="Contrôle d'identité biométrique complémentaire au badge"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Vérification faciale" }]}
      />

      <div className="mb-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-800">
        Démonstration académique : le traitement du visage est entièrement local au service IA
        (aucune donnée biométrique n'est transmise à un service tiers). Aucune gestion de consentement
        formelle n'est mise en œuvre — ne pas utiliser tel quel en production.
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="1. Employé et capture" description="Sélectionnez l'employé puis capturez son visage en direct" />
          <CardBody className="space-y-4">
            <div>
              <Label>Employé</Label>
              <Select
                value={employeeId}
                onChange={(e) => {
                  setEmployeeId(Number(e.target.value));
                  verify.reset();
                }}
                disabled={employees.isLoading}
              >
                <option value={0}>Sélectionner…</option>
                {(employees.data?.content ?? []).map((e) => (
                  <option key={e.id} value={e.id}>
                    {e.employeeNumber} — {e.firstName} {e.lastName}
                  </option>
                ))}
              </Select>
            </div>
            <WebcamCapture
              onCapture={(img) => {
                setImage(img);
                verify.reset();
              }}
            />
            <Button
              className="w-full"
              disabled={!employeeId || !image || verify.isPending}
              onClick={() => verify.mutate()}
            >
              <ScanFace className="size-4" />
              {verify.isPending ? "Vérification…" : "Vérifier l'identité"}
            </Button>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="2. Résultat" />
          <CardBody>
            {!result ? (
              <div className="flex flex-col items-center justify-center gap-3 py-12 text-center text-sm text-[var(--color-ink-500)]">
                <ScanFace className="size-10 text-ink-300" />
                Sélectionnez un employé, capturez son visage, puis lancez la vérification.
              </div>
            ) : (
              <div className="space-y-4 text-center">
                {result.matched ? (
                  <ShieldCheck className="mx-auto size-14 text-emerald-600" />
                ) : (
                  <ShieldX className="mx-auto size-14 text-red-600" />
                )}
                <div>
                  <StatusBadge tone={result.matched ? "success" : "danger"}>
                    {result.matched ? "Identité confirmée" : "Anomalie — visage non reconnu"}
                  </StatusBadge>
                </div>
                <p className="text-sm">{result.message}</p>
                <div className="grid grid-cols-2 gap-3 text-xs text-[var(--color-ink-500)]">
                  <div className="rounded-lg bg-[var(--color-surface-muted)] p-3">
                    <p className="text-base font-semibold text-[var(--color-ink-900)]">
                      {(result.confidence * 100).toFixed(0)}%
                    </p>
                    Confiance
                  </div>
                  <div className="rounded-lg bg-[var(--color-surface-muted)] p-3">
                    <p className="text-base font-semibold text-[var(--color-ink-900)]">{result.distance}</p>
                    Distance LBPH
                  </div>
                </div>
                {selectedEmployee ? (
                  <p className="text-xs text-[var(--color-ink-500)]">
                    Employé attendu : {selectedEmployee.firstName} {selectedEmployee.lastName} (
                    {selectedEmployee.employeeNumber})
                  </p>
                ) : null}
              </div>
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
