import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
import { zonesApi } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { ConfirmDialog, PageLoader } from "@/components/ui/Feedback";
import { formatDate } from "@/utils/cn";

export function ZoneDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const query = useQuery({ queryKey: ["zones", id], queryFn: () => zonesApi.get(Number(id)) });
  const remove = useMutation({
    mutationFn: () => zonesApi.remove(Number(id)),
    onSuccess: async () => {
      toast.success("Supprimé");
      await qc.invalidateQueries({ queryKey: ["zones"] });
      navigate("/zones");
    },
  });

  if (query.isLoading) return <PageLoader />;
  const entity = query.data;
  if (!entity) return <p>Introuvable</p>;

  return (
    <div>
      <PageHeader
        title={"Zone #" + entity.id}
        breadcrumbs={[{ label: "Zones", to: "/zones" }, { label: "Détail" }]}
        actions={
          <>
            <Link to={`/zones/${entity.id}/edit`}><Button variant="secondary">Éditer</Button></Link>
            <Button variant="danger" onClick={() => setOpen(true)}>Supprimer</Button>
          </>
        }
      />
      <Card>
        <CardBody className="space-y-2 text-sm">
          <p><strong>Nom:</strong> {entity.name}</p>
          <p><strong>Bâtiment:</strong> {entity.building}</p>
          <p><strong>Étage:</strong> {entity.floor}</p>
          <p><strong>Sensible:</strong> {entity.sensitive ? "Oui" : "Non"}</p>
          <p><strong>Description:</strong> {entity.description || "—"}</p>
          <p><strong>Créé:</strong> {formatDate(entity.createdAt)}</p>
          <p><strong>Mis à jour:</strong> {formatDate(entity.updatedAt)}</p>
        </CardBody>
      </Card>
      <ConfirmDialog open={open} title="Supprimer ?" description="Action irréversible." danger loading={remove.isPending} onClose={() => setOpen(false)} onConfirm={() => remove.mutate()} />
    </div>
  );
}
