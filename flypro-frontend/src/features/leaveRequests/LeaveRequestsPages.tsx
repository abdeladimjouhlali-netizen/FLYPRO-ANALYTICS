import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Check, X } from "lucide-react";
import { leaveRequestsApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { StatusBadge } from "@/components/ui/Card";
import { Label, Select } from "@/components/ui/FormControls";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { LeaveRequest, LeaveStatus, LeaveType } from "@/types/api";
import { formatDate } from "@/utils/cn";

const leaveTypeLabels: Record<LeaveType, string> = {
  ANNUAL: "Congé annuel",
  SICK: "Congé maladie",
  UNPAID: "Congé sans solde",
  OTHER: "Autre",
};

function statusTone(status: LeaveStatus) {
  if (status === "APPROVED") return "success" as const;
  if (status === "REJECTED") return "danger" as const;
  if (status === "PENDING") return "warning" as const;
  return "neutral" as const;
}

export function LeaveRequestsListPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const keyword = useDebouncedValue(search);
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["leave-requests", page, keyword, status],
    queryFn: () =>
      leaveRequestsApi.list({
        page,
        size: 10,
        keyword,
        status: (status || undefined) as LeaveStatus | undefined,
      }),
  });

  const review = useMutation({
    mutationFn: ({ id, action }: { id: number; action: "approve" | "reject" }) =>
      action === "approve" ? leaveRequestsApi.approve(id) : leaveRequestsApi.reject(id),
    onSuccess: (_, variables) => {
      toast.success(variables.action === "approve" ? "Congé approuvé" : "Congé refusé");
      queryClient.invalidateQueries({ queryKey: ["leave-requests"] });
    },
    onError: () => toast.error("Action impossible"),
  });

  const columns = useMemo<Column<LeaveRequest>[]>(
    () => [
      {
        key: "employee",
        header: "Employé",
        render: (r) => (
          <div>
            <p className="font-semibold">
              {r.employeeFirstName} {r.employeeLastName}
            </p>
            <p className="text-xs text-[var(--color-ink-500)]">{r.employeeNumber}</p>
          </div>
        ),
        exportValue: (r) => `${r.employeeFirstName} ${r.employeeLastName}`,
      },
      {
        key: "period",
        header: "Période",
        render: (r) => (
          <span>
            {formatDate(r.startDate)} → {formatDate(r.endDate)}
          </span>
        ),
        exportValue: (r) => `${r.startDate} - ${r.endDate}`,
      },
      { key: "type", header: "Type", render: (r) => leaveTypeLabels[r.type], exportValue: (r) => r.type },
      { key: "reason", header: "Motif", render: (r) => r.reason || "—", exportValue: (r) => r.reason ?? "" },
      {
        key: "status",
        header: "Statut",
        render: (r) => <StatusBadge tone={statusTone(r.status)}>{r.status}</StatusBadge>,
        exportValue: (r) => r.status,
      },
      {
        key: "actions",
        header: "Actions",
        render: (r) =>
          r.status === "PENDING" ? (
            <div className="flex gap-2">
              <Button
                size="sm"
                disabled={review.isPending}
                onClick={() => review.mutate({ id: r.id, action: "approve" })}
              >
                <Check className="size-4" />
                Approuver
              </Button>
              <Button
                size="sm"
                variant="ghost"
                disabled={review.isPending}
                onClick={() => review.mutate({ id: r.id, action: "reject" })}
              >
                <X className="size-4" />
                Refuser
              </Button>
            </div>
          ) : (
            <span className="text-xs text-[var(--color-ink-500)]">
              {r.reviewedBy ? `Par ${r.reviewedBy}` : "—"}
            </span>
          ),
      },
    ],
    [review],
  );

  return (
    <div>
      <PageHeader
        title="Demandes de congé"
        description="Approuvez ou refusez les demandes de congé — un congé approuvé bloque l'accès badge de l'employé pendant sa durée"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Demandes de congé" }]}
      />
      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <div>
          <Label>Statut</Label>
          <Select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tous</option>
            {(["PENDING", "APPROVED", "REJECTED", "CANCELLED"] as LeaveStatus[]).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
        </div>
      </div>
      <DataTable
        columns={columns}
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        search={search}
        onSearchChange={setSearch}
        page={page}
        size={10}
        totalElements={query.data?.totalElements ?? 0}
        totalPages={query.data?.totalPages ?? 0}
        onPageChange={setPage}
        exportFileName="demandes-de-conge.csv"
      />
    </div>
  );
}
