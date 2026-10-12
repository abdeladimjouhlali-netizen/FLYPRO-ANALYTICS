import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Plus } from "lucide-react";
import { departmentsApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/Feedback";
import { StatusBadge } from "@/components/ui/Card";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { Department } from "@/types/api";


export function DepartmentsListPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("id,desc");
  const [toDelete, setToDelete] = useState<Department | null>(null);
  const keyword = useDebouncedValue(search);

  const query = useQuery({
    queryKey: ["departments", page, keyword, sort],
    queryFn: () => departmentsApi.list({ page, size: 10, keyword, sort }),
  });

  const remove = useMutation({
    mutationFn: (id: number) => departmentsApi.remove(id),
    onSuccess: async () => {
      toast.success("Département supprimé");
      setToDelete(null);
      await qc.invalidateQueries({ queryKey: ["departments"] });
    },
  });

  const columns = useMemo<Column<Department>[]>(() => [
    { key: "code", header: "Code", sortable: true, render: (r) => r.code, exportValue: (r) => r.code },
    { key: "name", header: "Nom", sortable: true, render: (r) => r.name, exportValue: (r) => r.name },
    { key: "active", header: "Actif", render: (r) => <StatusBadge tone={r.active ? "success" : "neutral"}>{r.active ? "Oui" : "Non"}</StatusBadge>, exportValue: (r) => (r.active ? "Oui" : "Non") },
    {
      key: "actions",
      header: "Actions",
      render: (r) => (
        <div className="flex gap-2">
          <Link className="text-brand-700 hover:underline" to={`/departments/${r.id}`}>Détail</Link>
          <Link className="text-brand-700 hover:underline" to={`/departments/${r.id}/edit`}>Éditer</Link>
          <button type="button" className="text-[var(--color-danger)] hover:underline" onClick={() => setToDelete(r)}>Supprimer</button>
        </div>
      ),
    },
  ], []);

  return (
    <div>
      <PageHeader
        title="Départements"
        description="Gestion des départements"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Départements" }]}
        actions={<Link to="/departments/new"><Button><Plus className="size-4" /> Nouveau</Button></Link>}
      />
      <DataTable
        columns={columns}
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        search={search}
        onSearchChange={setSearch}
        page={page}
        size={query.data?.size ?? 10}
        totalElements={query.data?.totalElements ?? 0}
        totalPages={query.data?.totalPages ?? 0}
        onPageChange={setPage}
        sort={sort}
        onSortChange={setSort}
        exportFileName="departments.csv"
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Confirmer la suppression"
        description={`Supprimer département ${toDelete?.name ?? ""} ?`}
        danger
        loading={remove.isPending}
        onClose={() => setToDelete(null)}
        onConfirm={() => {
          if (toDelete) remove.mutate(toDelete.id);
        }}
      />
    </div>
  );
}
