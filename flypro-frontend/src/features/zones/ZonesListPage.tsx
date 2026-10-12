import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Plus } from "lucide-react";
import { zonesApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/Feedback";
import { StatusBadge } from "@/components/ui/Card";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { Zone } from "@/types/api";


export function ZonesListPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("id,desc");
  const [toDelete, setToDelete] = useState<Zone | null>(null);
  const keyword = useDebouncedValue(search);

  const query = useQuery({
    queryKey: ["zones", page, keyword, sort],
    queryFn: () => zonesApi.list({ page, size: 10, keyword, sort }),
  });

  const remove = useMutation({
    mutationFn: (id: number) => zonesApi.remove(id),
    onSuccess: async () => {
      toast.success("Zone supprimé");
      setToDelete(null);
      await qc.invalidateQueries({ queryKey: ["zones"] });
    },
  });

  const columns = useMemo<Column<Zone>[]>(() => [
    { key: "name", header: "Nom", sortable: true, render: (r) => r.name, exportValue: (r) => r.name },
    { key: "building", header: "Bâtiment", render: (r) => r.building, exportValue: (r) => r.building },
    { key: "floor", header: "Étage", render: (r) => r.floor, exportValue: (r) => r.floor },
    { key: "sensitive", header: "Sensible", render: (r) => <StatusBadge tone={r.sensitive ? "danger" : "neutral"}>{r.sensitive ? "Oui" : "Non"}</StatusBadge>, exportValue: (r) => (r.sensitive ? "Oui" : "Non") },
    {
      key: "actions",
      header: "Actions",
      render: (r) => (
        <div className="flex gap-2">
          <Link className="text-brand-700 hover:underline" to={`/zones/${r.id}`}>Détail</Link>
          <Link className="text-brand-700 hover:underline" to={`/zones/${r.id}/edit`}>Éditer</Link>
          <button type="button" className="text-[var(--color-danger)] hover:underline" onClick={() => setToDelete(r)}>Supprimer</button>
        </div>
      ),
    },
  ], []);

  return (
    <div>
      <PageHeader
        title="Zones"
        description="Gestion des zones"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Zones" }]}
        actions={<Link to="/zones/new"><Button><Plus className="size-4" /> Nouveau</Button></Link>}
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
        exportFileName="zones.csv"
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Confirmer la suppression"
        description={`Supprimer zone ${toDelete?.name ?? ""} ?`}
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
