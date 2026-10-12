import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { Activity, AlertTriangle, Building2, CalendarClock, IdCard, Mail, Phone, ShieldCheck, UserRound, X } from "lucide-react";
import { employeePortalApi } from "@/api/services";
import { BadgeCard3D } from "@/components/badges/BadgeCard3D";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { EmptyState, PageLoader } from "@/components/ui/Feedback";
import { FieldError, Input, Label, Select, Textarea } from "@/components/ui/FormControls";
import { useAuth } from "@/contexts/AuthContext";
import type { AlertSeverity, LeaveStatus, LeaveType } from "@/types/api";
import { formatDate } from "@/utils/cn";

const leaveTypeLabels: Record<LeaveType, string> = {
  ANNUAL: "Congé annuel",
  SICK: "Congé maladie",
  UNPAID: "Congé sans solde",
  OTHER: "Autre",
};

const leaveStatusTone: Record<LeaveStatus, "success" | "warning" | "danger" | "neutral"> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "danger",
  CANCELLED: "neutral",
};

const leaveStatusLabels: Record<LeaveStatus, string> = {
  PENDING: "En attente",
  APPROVED: "Approuvée",
  REJECTED: "Refusée",
  CANCELLED: "Annulée",
};

const leaveSchema = z
  .object({
    startDate: z.string().min(1, "Date de début requise"),
    endDate: z.string().min(1, "Date de fin requise"),
    type: z.enum(["ANNUAL", "SICK", "UNPAID", "OTHER"]),
    reason: z.string().max(1000).optional(),
  })
  .refine((v) => v.endDate >= v.startDate, {
    message: "La date de fin doit être après la date de début",
    path: ["endDate"],
  });

type LeaveFormValues = z.infer<typeof leaveSchema>;

function severityTone(s: AlertSeverity) {
  if (s === "CRITICAL" || s === "HIGH") return "danger" as const;
  if (s === "MEDIUM") return "warning" as const;
  return "neutral" as const;
}

function directionLabel(direction: string) {
  if (direction === "ENTRY") return "Entrée";
  if (direction === "EXIT") return "Sortie";
  return "Neutre";
}

function resultTone(result: string) {
  return result === "AUTHORIZED" ? ("success" as const) : ("danger" as const);
}

export function EmployeeDashboardPage() {
  const { user } = useAuth();
  const meQuery = useQuery({ queryKey: ["me", "employee"], queryFn: employeePortalApi.me });
  const badgesQuery = useQuery({ queryKey: ["me", "badges"], queryFn: employeePortalApi.badges });
  const alertsQuery = useQuery({ queryKey: ["me", "alerts"], queryFn: employeePortalApi.alerts });
  const eventsQuery = useQuery({
    queryKey: ["me", "access-events", 0],
    queryFn: () => employeePortalApi.accessEvents(0, 5),
  });

  if (meQuery.isLoading) return <PageLoader />;

  const employee = meQuery.data;
  const activeBadges = (badgesQuery.data ?? []).filter((b) => b.status === "ACTIVE").length;
  const openAlerts = (alertsQuery.data ?? []).filter((a) => a.status === "OPEN" || a.status === "ACKNOWLEDGED").length;
  const events = eventsQuery.data?.content ?? [];

  return (
    <div>
      <PageHeader
        title={`Bonjour, ${user?.username ?? ""}`}
        description={employee ? `${employee.position} — ${employee.department.name}` : undefined}
        breadcrumbs={[{ label: "Portail employé" }]}
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <Card>
          <CardBody className="flex items-center gap-3">
            <span className="flex size-11 items-center justify-center rounded-xl bg-brand-100 text-brand-700 dark:bg-brand-900/40 dark:text-brand-200">
              <IdCard className="size-5" />
            </span>
            <div>
              <p className="text-2xl font-bold tabular-nums">{activeBadges}</p>
              <p className="text-xs text-[var(--color-ink-500)]">Badge(s) actif(s)</p>
            </div>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="flex items-center gap-3">
            <span className="flex size-11 items-center justify-center rounded-xl bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-200">
              <AlertTriangle className="size-5" />
            </span>
            <div>
              <p className="text-2xl font-bold tabular-nums">{openAlerts}</p>
              <p className="text-xs text-[var(--color-ink-500)]">Alerte(s) en cours</p>
            </div>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="flex items-center gap-3">
            <span className="flex size-11 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-200">
              <ShieldCheck className="size-5" />
            </span>
            <div>
              <p className="text-2xl font-bold tabular-nums">{employee?.status === "ACTIVE" ? "Actif" : employee?.status ?? "—"}</p>
              <p className="text-xs text-[var(--color-ink-500)]">Statut du compte</p>
            </div>
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader
          title="Mes derniers accès"
          description="5 derniers événements enregistrés sur vos badges"
          action={<Activity className="size-4 text-brand-600" />}
        />
        <CardBody className="space-y-2">
          {eventsQuery.isLoading ? (
            <PageLoader />
          ) : events.length === 0 ? (
            <EmptyState title="Aucun accès enregistré" description="Vos passages apparaîtront ici." />
          ) : (
            events.map((e) => (
              <div
                key={e.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm"
              >
                <div>
                  <p className="font-semibold">{e.accessPointName || "Point d'accès"}</p>
                  <p className="text-xs text-[var(--color-ink-500)]">
                    {formatDate(e.eventTimestamp)} · {directionLabel(e.direction)}
                    {e.zone ? ` · ${e.zone.name}` : ""}
                  </p>
                </div>
                <StatusBadge tone={resultTone(e.result)}>{e.result}</StatusBadge>
              </div>
            ))
          )}
        </CardBody>
      </Card>
    </div>
  );
}

export function EmployeeBadgesPage() {
  const meQuery = useQuery({ queryKey: ["me", "employee"], queryFn: employeePortalApi.me });
  const badgesQuery = useQuery({ queryKey: ["me", "badges"], queryFn: employeePortalApi.badges });

  if (badgesQuery.isLoading || meQuery.isLoading) return <PageLoader />;

  const badges = badgesQuery.data ?? [];
  const employee = meQuery.data;

  return (
    <div>
      <PageHeader
        title="Mes badges"
        description="Badges physiques associés à votre profil"
        breadcrumbs={[{ label: "Portail employé", to: "/employee" }, { label: "Mes badges" }]}
      />
      {badges.length === 0 ? (
        <EmptyState title="Aucun badge assigné" description="Contactez l'administration pour l'affectation d'un badge." />
      ) : (
        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
          {badges.map((b) => (
            <BadgeCard3D
              key={b.id}
              allowPhotoUpload
              data={{
                badgeNumber: b.badgeNumber,
                type: b.type,
                status: b.status,
                issueDate: b.issueDate,
                expirationDate: b.expirationDate ?? undefined,
                employeeId: employee?.id,
                employeeFirstName: employee?.firstName,
                employeeLastName: employee?.lastName,
                employeeNumber: employee?.employeeNumber,
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export function EmployeeAlertsPage() {
  const alertsQuery = useQuery({ queryKey: ["me", "alerts"], queryFn: employeePortalApi.alerts });

  if (alertsQuery.isLoading) return <PageLoader />;
  const alerts = alertsQuery.data ?? [];

  return (
    <div>
      <PageHeader
        title="Mes alertes"
        description="Alertes de sécurité concernant vos accès"
        breadcrumbs={[{ label: "Portail employé", to: "/employee" }, { label: "Mes alertes" }]}
      />
      {alerts.length === 0 ? (
        <EmptyState title="Aucune alerte" description="Tout est en ordre." />
      ) : (
        <div className="space-y-3">
          {alerts.map((a) => (
            <Card key={a.id}>
              <CardBody className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="mb-1 flex items-center gap-2">
                    <StatusBadge tone={severityTone(a.severity)}>{a.severity}</StatusBadge>
                    <StatusBadge>{a.status}</StatusBadge>
                  </div>
                  <p className="font-semibold">{a.title}</p>
                  {a.description ? <p className="mt-1 text-sm text-[var(--color-ink-500)]">{a.description}</p> : null}
                </div>
                <p className="text-xs text-[var(--color-ink-500)]">{formatDate(a.createdDate)}</p>
              </CardBody>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

export function EmployeeProfilePage() {
  const { user } = useAuth();
  const meQuery = useQuery({ queryKey: ["me", "employee"], queryFn: employeePortalApi.me });

  if (meQuery.isLoading) return <PageLoader />;
  const employee = meQuery.data;
  if (!employee) return <p>Introuvable</p>;

  return (
    <div>
      <PageHeader
        title="Mon profil"
        breadcrumbs={[{ label: "Portail employé", to: "/employee" }, { label: "Mon profil" }]}
      />
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader
            title={`${employee.firstName} ${employee.lastName}`}
            description={employee.position}
            action={<UserRound className="size-5 text-brand-600" />}
          />
          <CardBody className="space-y-3 text-sm">
            <p className="flex items-center gap-2">
              <IdCard className="size-4 text-[var(--color-ink-400)]" /> Matricule : <strong>{employee.employeeNumber}</strong>
            </p>
            <p className="flex items-center gap-2">
              <Building2 className="size-4 text-[var(--color-ink-400)]" /> Département : <strong>{employee.department.name}</strong>
            </p>
            <p className="flex items-center gap-2">
              <Mail className="size-4 text-[var(--color-ink-400)]" /> {employee.email}
            </p>
            {employee.phone ? (
              <p className="flex items-center gap-2">
                <Phone className="size-4 text-[var(--color-ink-400)]" /> {employee.phone}
              </p>
            ) : null}
            <p>
              <strong>Statut :</strong> <StatusBadge tone={employee.status === "ACTIVE" ? "success" : "neutral"}>{employee.status}</StatusBadge>
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardHeader title="Compte portail" description="Identifiants de connexion" />
          <CardBody className="space-y-2 text-sm">
            <p>
              <strong>Identifiant :</strong> {user?.username}
            </p>
            <p>
              <strong>Email :</strong> {user?.email}
            </p>
            <p>
              <strong>Compte verrouillé :</strong> {user?.accountLocked ? "Oui" : "Non"}
            </p>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}

export function EmployeeLeaveRequestsPage() {
  const queryClient = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const leavesQuery = useQuery({
    queryKey: ["me", "leave-requests"],
    queryFn: () => employeePortalApi.leaveRequests(0, 50),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<LeaveFormValues>({
    resolver: zodResolver(leaveSchema),
    defaultValues: { type: "ANNUAL" },
  });

  const submit = useMutation({
    mutationFn: employeePortalApi.requestLeave,
    onSuccess: () => {
      toast.success("Demande de congé envoyée");
      queryClient.invalidateQueries({ queryKey: ["me", "leave-requests"] });
      reset({ startDate: "", endDate: "", type: "ANNUAL", reason: "" });
      setFormOpen(false);
    },
    onError: (err: unknown) => {
      const ax = err as { response?: { data?: { message?: string } } };
      toast.error(ax.response?.data?.message ?? "Impossible d'envoyer la demande de congé");
    },
  });

  const cancel = useMutation({
    mutationFn: employeePortalApi.cancelLeaveRequest,
    onSuccess: () => {
      toast.success("Demande annulée");
      queryClient.invalidateQueries({ queryKey: ["me", "leave-requests"] });
    },
    onError: () => toast.error("Impossible d'annuler cette demande"),
  });

  const requests = leavesQuery.data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Mes demandes de congé"
        description="Demandez un congé — tant qu'elle est approuvée, votre badge ne fonctionnera pas pendant cette période"
        breadcrumbs={[{ label: "Portail employé", to: "/employee" }, { label: "Mes congés" }]}
        actions={
          <Button onClick={() => setFormOpen((v) => !v)}>
            <CalendarClock className="size-4" />
            {formOpen ? "Fermer" : "Nouvelle demande"}
          </Button>
        }
      />

      {formOpen ? (
        <Card className="mb-6">
          <CardHeader title="Nouvelle demande de congé" />
          <CardBody>
            <form
              className="grid gap-4 sm:grid-cols-2"
              onSubmit={handleSubmit((values) => submit.mutate(values))}
            >
              <div>
                <Label htmlFor="startDate">Date de début</Label>
                <Input id="startDate" type="date" {...register("startDate")} />
                <FieldError message={errors.startDate?.message} />
              </div>
              <div>
                <Label htmlFor="endDate">Date de fin</Label>
                <Input id="endDate" type="date" {...register("endDate")} />
                <FieldError message={errors.endDate?.message} />
              </div>
              <div>
                <Label htmlFor="type">Type de congé</Label>
                <Select id="type" {...register("type")}>
                  {(Object.keys(leaveTypeLabels) as LeaveType[]).map((t) => (
                    <option key={t} value={t}>
                      {leaveTypeLabels[t]}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="sm:col-span-2">
                <Label htmlFor="reason">Motif (optionnel)</Label>
                <Textarea id="reason" placeholder="Précisez le motif de votre demande" {...register("reason")} />
                <FieldError message={errors.reason?.message} />
              </div>
              <div className="sm:col-span-2">
                <Button disabled={submit.isPending}>
                  {submit.isPending ? "Envoi…" : "Envoyer la demande"}
                </Button>
              </div>
            </form>
          </CardBody>
        </Card>
      ) : null}

      {leavesQuery.isLoading ? (
        <PageLoader />
      ) : requests.length === 0 ? (
        <EmptyState title="Aucune demande de congé" description="Vos demandes apparaîtront ici." />
      ) : (
        <div className="space-y-3">
          {requests.map((r) => (
            <Card key={r.id}>
              <CardBody className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="mb-1 flex items-center gap-2">
                    <StatusBadge tone={leaveStatusTone[r.status]}>{leaveStatusLabels[r.status]}</StatusBadge>
                    <span className="text-xs text-[var(--color-ink-500)]">{leaveTypeLabels[r.type]}</span>
                  </div>
                  <p className="font-semibold">
                    {formatDate(r.startDate)} → {formatDate(r.endDate)}
                  </p>
                  {r.reason ? <p className="mt-1 text-sm text-[var(--color-ink-500)]">{r.reason}</p> : null}
                  {r.reviewComment ? (
                    <p className="mt-1 text-xs text-[var(--color-ink-500)]">
                      Commentaire admin : {r.reviewComment}
                    </p>
                  ) : null}
                </div>
                {r.status === "PENDING" ? (
                  <Button
                    variant="ghost"
                    onClick={() => cancel.mutate(r.id)}
                    disabled={cancel.isPending}
                  >
                    <X className="size-4" />
                    Annuler
                  </Button>
                ) : null}
              </CardBody>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
