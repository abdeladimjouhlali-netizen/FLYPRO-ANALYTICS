import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { motion } from "framer-motion";
import { Bot, Gauge, ShieldAlert, Sparkles } from "lucide-react";
import { analyticsAiApi } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { Checkbox, FieldError, Input, Label, Select } from "@/components/ui/FormControls";
import type { AiPrediction } from "@/types/api";

const schema = z.object({
  eventTimestamp: z.string().min(1),
  employeeId: z.number().min(1),
  badgeId: z.number().min(1),
  accessPointId: z.number().min(1),
  zoneId: z.number().min(1),
  accessResult: z.enum(["AUTHORIZED", "DENIED", "EXPIRED", "UNKNOWN", "TECHNICAL_ERROR"]),
  accessDirection: z.enum(["ENTRY", "EXIT"]),
  deniedLastHour: z.number().min(0),
  accessesToday: z.number().min(0),
  employeeStatus: z.enum(["ACTIVE", "INACTIVE", "SUSPENDED", "ARCHIVED"]),
  badgeStatus: z.enum(["ACTIVE", "SUSPENDED", "EXPIRED", "REVOKED", "LOST", "PENDING_ACTIVATION"]),
  isExpectedZone: z.boolean(),
});
type FormValues = z.infer<typeof schema>;

function ProbabilityGauge({ value, label, id }: { value: number; label: string; id: string }) {
  const pct = Math.max(0, Math.min(100, value));
  const r = 54;
  const c = 2 * Math.PI * r;
  const offset = c - (pct / 100) * c;
  return (
    <div className="flex flex-col items-center">
      <div className="relative size-36">
        <svg viewBox="0 0 140 140" className="size-full -rotate-90">
          <circle cx="70" cy="70" r={r} fill="none" stroke="currentColor" strokeWidth="10" className="text-slate-200 dark:text-navy-700" />
          <circle
            cx="70"
            cy="70"
            r={r}
            fill="none"
            stroke={`url(#${id})`}
            strokeWidth="10"
            strokeLinecap="round"
            strokeDasharray={c}
            strokeDashoffset={offset}
          />
          <defs>
            <linearGradient id={id} x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor="#ed5674" />
              <stop offset="100%" stopColor="#C2002F" />
            </linearGradient>
          </defs>
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-semibold tabular-nums text-navy-900 dark:text-white">{pct.toFixed(0)}%</span>
          <span className="text-[10px] font-semibold tracking-wide text-ink-500 uppercase">{label}</span>
        </div>
      </div>
    </div>
  );
}

export function AnalyticsPage() {
  const health = useQuery({
    queryKey: ["ai", "health"],
    queryFn: analyticsAiApi.health,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: import.meta.env.VITE_ENABLE_AI !== "false",
  });
  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      eventTimestamp: new Date().toISOString().slice(0, 16),
      employeeId: 1,
      badgeId: 1,
      accessPointId: 1,
      zoneId: 1,
      accessResult: "DENIED",
      accessDirection: "ENTRY",
      deniedLastHour: 3,
      accessesToday: 10,
      employeeStatus: "ACTIVE",
      badgeStatus: "ACTIVE",
      isExpectedZone: true,
    },
  });

  const predict = useMutation({
    mutationFn: (values: FormValues) =>
      analyticsAiApi.predict({
        ...values,
        eventTimestamp: new Date(values.eventTimestamp).toISOString(),
      }),
    onSuccess: () => toast.success("Prédiction calculée"),
  });

  const result = predict.data as AiPrediction | undefined;

  return (
    <div>
      <PageHeader
        title="Analytics AI"
        description="Détection d’anomalies — Isolation Forest · risk scoring · recommandations"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Analytics AI" }]}
      />

      <Card className="mb-6 overflow-hidden">
        <div className="bg-gradient-to-r from-brand-900 via-brand-600 to-brand-500 px-5 py-4 text-white">
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <Bot className="size-5" />
            <span className="ram-wordmark text-base text-white">service ai</span>
            {health.isSuccess ? (
              <StatusBadge tone="success">En ligne</StatusBadge>
            ) : (
              <StatusBadge tone="danger">Hors ligne</StatusBadge>
            )}
            {health.data ? (
              <span className="text-white/70">modelLoaded={String(health.data.modelLoaded)}</span>
            ) : null}
            {import.meta.env.VITE_ENABLE_AI === "false" ? (
              <span className="text-white/70">(désactivé en mode local)</span>
            ) : null}
          </div>
        </div>
      </Card>

      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader title="Événement à analyser" description="Paramètres d’entrée du modèle" />
          <CardBody>
            <form className="grid gap-3 md:grid-cols-2" onSubmit={handleSubmit((v) => predict.mutate(v))}>
              <div className="md:col-span-2">
                <Label>Horodatage</Label>
                <Input type="datetime-local" {...register("eventTimestamp")} />
                <FieldError message={errors.eventTimestamp?.message} />
              </div>
              <div><Label>Employee ID</Label><Input type="number" {...register("employeeId", { valueAsNumber: true })} /></div>
              <div><Label>Badge ID</Label><Input type="number" {...register("badgeId", { valueAsNumber: true })} /></div>
              <div><Label>Access Point ID</Label><Input type="number" {...register("accessPointId", { valueAsNumber: true })} /></div>
              <div><Label>Zone ID</Label><Input type="number" {...register("zoneId", { valueAsNumber: true })} /></div>
              <div>
                <Label>Résultat</Label>
                <Select {...register("accessResult")}>
                  <option value="AUTHORIZED">AUTHORIZED</option>
                  <option value="DENIED">DENIED</option>
                  <option value="EXPIRED">EXPIRED</option>
                </Select>
              </div>
              <div>
                <Label>Direction</Label>
                <Select {...register("accessDirection")}>
                  <option value="ENTRY">ENTRY</option>
                  <option value="EXIT">EXIT</option>
                </Select>
              </div>
              <div><Label>Refus (1h)</Label><Input type="number" {...register("deniedLastHour", { valueAsNumber: true })} /></div>
              <div><Label>Accès du jour</Label><Input type="number" {...register("accessesToday", { valueAsNumber: true })} /></div>
              <div>
                <Label>Statut employé</Label>
                <Select {...register("employeeStatus")}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                  <option value="SUSPENDED">SUSPENDED</option>
                  <option value="ARCHIVED">ARCHIVED</option>
                </Select>
              </div>
              <div>
                <Label>Statut badge</Label>
                <Select {...register("badgeStatus")}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="REVOKED">REVOKED</option>
                  <option value="EXPIRED">EXPIRED</option>
                  <option value="SUSPENDED">SUSPENDED</option>
                </Select>
              </div>
              <div className="md:col-span-2">
                <Checkbox
                  label="Zone attendue (profil)"
                  checked={watch("isExpectedZone")}
                  onChange={(e) => setValue("isExpectedZone", e.target.checked)}
                />
              </div>
              <div className="md:col-span-2">
                <Button type="submit" disabled={predict.isPending || import.meta.env.VITE_ENABLE_AI === "false"}>
                  <Sparkles className="size-4" />
                  {predict.isPending ? "Analyse…" : "Lancer la prédiction"}
                </Button>
              </div>
            </form>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Résultat mission" description="Risk score · confiance · action recommandée" />
          <CardBody>
            {!result ? (
              <div className="flex flex-col items-center justify-center gap-3 py-12 text-center">
                <Gauge className="size-10 text-ink-300" />
                <p className="text-sm text-ink-500">Aucune prédiction. Lancez une analyse d’événement.</p>
              </div>
            ) : (
              <motion.div
                className="space-y-6"
                initial={{ opacity: 0, scale: 0.98 }}
                animate={{ opacity: 1, scale: 1 }}
              >
                <div className="flex items-center justify-between">
                  <StatusBadge tone={result.prediction === "ANOMALY" ? "danger" : "success"}>
                    {result.prediction}
                  </StatusBadge>
                  <span className="text-xs text-ink-500">{result.modelVersion}</span>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <ProbabilityGauge id="risk-gauge" value={result.riskScore * 100} label="Risk" />
                  <ProbabilityGauge id="conf-gauge" value={result.confidence} label="Confiance" />
                </div>
                <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4">
                  <p className="flex items-center gap-2 text-sm font-semibold">
                    <ShieldAlert className="size-4 text-amber-600" /> Raison
                  </p>
                  <p className="mt-1 text-sm text-ink-600">{result.reason}</p>
                </div>
                <div>
                  <p className="text-sm font-semibold">Action recommandée</p>
                  <p className="mt-1 text-sm text-navy-900 dark:text-white">{result.recommendedAction}</p>
                </div>
                {result.ruleFlags?.length ? (
                  <div className="flex flex-wrap gap-2">
                    {result.ruleFlags.map((f) => (
                      <StatusBadge key={f} tone="warning">
                        {f}
                      </StatusBadge>
                    ))}
                  </div>
                ) : null}
              </motion.div>
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
