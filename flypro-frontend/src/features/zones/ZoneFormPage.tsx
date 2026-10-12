import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { zonesApi } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { Checkbox, FieldError, Input, Label, Select, Textarea } from "@/components/ui/FormControls";
import { PageLoader } from "@/components/ui/Feedback";
import type { ZoneRequest } from "@/types/api";


const schema = z.object({ name: z.string().min(2), building: z.string().min(1), floor: z.string().min(1), sensitive: z.boolean(), description: z.string().optional() });
type FormValues = z.infer<typeof schema>;

export function ZoneFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const entityQuery = useQuery({
    queryKey: ["zones", id],
    queryFn: () => zonesApi.get(Number(id)),
    enabled: isEdit,
  });

  const { register, handleSubmit, reset, setValue, watch, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", building: "", floor: "", sensitive: false, description: "" },
  });

  useEffect(() => {
    if (entityQuery.data) {
      const e = entityQuery.data;
      reset({
        name: e.name,
        building: e.building,
        floor: e.floor,
        sensitive: e.sensitive,
        description: e.description ?? "",
      });
    }
  }, [entityQuery.data, reset]);

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      isEdit ? zonesApi.update(Number(id), values as ZoneRequest) : zonesApi.create(values as ZoneRequest),
    onSuccess: async (saved) => {
      toast.success(isEdit ? "Mis à jour" : "Créé");
      await qc.invalidateQueries({ queryKey: ["zones"] });
      navigate(`/zones/${saved.id}`);
    },
  });

  if (isEdit && entityQuery.isLoading) return <PageLoader />;

  return (
    <div>
      <PageHeader
        title={isEdit ? "Modifier zone" : "Nouveau zone"}
        breadcrumbs={[{ label: "Zones", to: "/zones" }, { label: isEdit ? "Édition" : "Création" }]}
      />
      <Card>
        <CardBody>
          <form className="grid max-w-2xl gap-4" onSubmit={handleSubmit((v) => save.mutate(v))}>
            <div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
            <div><Label>Bâtiment</Label><Input {...register("building")} /><FieldError message={errors.building?.message} /></div>
            <div><Label>Étage</Label><Input {...register("floor")} /><FieldError message={errors.floor?.message} /></div>
            <div><Label>Description</Label><Textarea {...register("description")} /></div>
            <Checkbox label="Zone sensible" checked={!!watch("sensitive")} onChange={(e) => setValue("sensitive", e.target.checked)} />
            <div className="flex gap-2">
              <Button type="submit" disabled={save.isPending}>{save.isPending ? "Enregistrement…" : "Enregistrer"}</Button>
              <Button type="button" variant="secondary" onClick={() => navigate("/zones")}>Annuler</Button>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
