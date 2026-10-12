import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { departmentsApi } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { Checkbox, FieldError, Input, Label, Select, Textarea } from "@/components/ui/FormControls";
import { PageLoader } from "@/components/ui/Feedback";
import type { DepartmentRequest } from "@/types/api";


const schema = z.object({ name: z.string().min(2), code: z.string().min(2), description: z.string().optional() });
type FormValues = z.infer<typeof schema>;

export function DepartmentFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const entityQuery = useQuery({
    queryKey: ["departments", id],
    queryFn: () => departmentsApi.get(Number(id)),
    enabled: isEdit,
  });

  const { register, handleSubmit, reset, setValue, watch, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", code: "", description: "" },
  });

  useEffect(() => {
    if (entityQuery.data) {
      const e = entityQuery.data;
      reset({ name: e.name, code: e.code, description: e.description ?? "" });
    }
  }, [entityQuery.data, reset]);

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      isEdit ? departmentsApi.update(Number(id), values as DepartmentRequest) : departmentsApi.create(values as DepartmentRequest),
    onSuccess: async (saved) => {
      toast.success(isEdit ? "Mis à jour" : "Créé");
      await qc.invalidateQueries({ queryKey: ["departments"] });
      navigate(`/departments/${saved.id}`);
    },
  });

  if (isEdit && entityQuery.isLoading) return <PageLoader />;

  return (
    <div>
      <PageHeader
        title={isEdit ? "Modifier département" : "Nouveau département"}
        breadcrumbs={[{ label: "Départements", to: "/departments" }, { label: isEdit ? "Édition" : "Création" }]}
      />
      <Card>
        <CardBody>
          <form className="grid max-w-2xl gap-4" onSubmit={handleSubmit((v) => save.mutate(v))}>
            <div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
            <div><Label>Code</Label><Input {...register("code")} /><FieldError message={errors.code?.message} /></div>
            <div><Label>Description</Label><Textarea {...register("description")} /></div>
            <div className="flex gap-2">
              <Button type="submit" disabled={save.isPending}>{save.isPending ? "Enregistrement…" : "Enregistrer"}</Button>
              <Button type="button" variant="secondary" onClick={() => navigate("/departments")}>Annuler</Button>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
