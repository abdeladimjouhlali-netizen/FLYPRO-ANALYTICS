import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const src = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../src");
const w = (rel, content) => {
  const full = path.join(src, rel);
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, content.trimStart());
};

w(
  "hooks/useDebouncedValue.ts",
  `
import { useEffect, useState } from "react";

export function useDebouncedValue<T>(value: T, delay = 300) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(id);
  }, [value, delay]);
  return debounced;
}
`,
);

const modules = [
  {
    folder: "departments",
    entity: "Department",
    api: "departmentsApi",
    basePath: "/departments",
    title: "Départements",
    singular: "département",
    columns: `
      { key: "code", header: "Code", sortable: true, render: (r) => r.code, exportValue: (r) => r.code },
      { key: "name", header: "Nom", sortable: true, render: (r) => r.name, exportValue: (r) => r.name },
      { key: "active", header: "Actif", render: (r) => <StatusBadge tone={r.active ? "success" : "neutral"}>{r.active ? "Oui" : "Non"}</StatusBadge>, exportValue: (r) => r.active ? "Oui" : "Non" },
    `,
    formFields: `
      <div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
      <div><Label>Code</Label><Input {...register("code")} /><FieldError message={errors.code?.message} /></div>
      <div><Label>Description</Label><Textarea {...register("description")} /></div>
    `,
    schema: `z.object({ name: z.string().min(2), code: z.string().min(2), description: z.string().optional() })`,
    defaults: `{ name: "", code: "", description: "" }`,
    mapEntity: `(e) => ({ name: e.name, code: e.code, description: e.description ?? "" })`,
    detailFields: `
      <p><strong>Code:</strong> {entity.code}</p>
      <p><strong>Nom:</strong> {entity.name}</p>
      <p><strong>Description:</strong> {entity.description || "—"}</p>
      <p><strong>Actif:</strong> {entity.active ? "Oui" : "Non"}</p>
    `,
    typeImport: "Department, DepartmentRequest",
  },
  {
    folder: "zones",
    entity: "Zone",
    api: "zonesApi",
    basePath: "/zones",
    title: "Zones",
    singular: "zone",
    columns: `
      { key: "name", header: "Nom", sortable: true, render: (r) => r.name, exportValue: (r) => r.name },
      { key: "building", header: "Bâtiment", render: (r) => r.building, exportValue: (r) => r.building },
      { key: "floor", header: "Étage", render: (r) => r.floor, exportValue: (r) => r.floor },
      { key: "sensitive", header: "Sensible", render: (r) => <StatusBadge tone={r.sensitive ? "danger" : "neutral"}>{r.sensitive ? "Oui" : "Non"}</StatusBadge>, exportValue: (r) => r.sensitive ? "Oui" : "Non" },
    `,
    formFields: `
      <div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
      <div><Label>Bâtiment</Label><Input {...register("building")} /><FieldError message={errors.building?.message} /></div>
      <div><Label>Étage</Label><Input {...register("floor")} /><FieldError message={errors.floor?.message} /></div>
      <div><Label>Description</Label><Textarea {...register("description")} /></div>
      <Checkbox label="Zone sensible" checked={!!watch("sensitive")} onChange={(e) => setValue("sensitive", e.target.checked)} />
    `,
    schema: `z.object({ name: z.string().min(2), building: z.string().min(1), floor: z.string().min(1), sensitive: z.boolean(), description: z.string().optional() })`,
    defaults: `{ name: "", building: "", floor: "", sensitive: false, description: "" }`,
    mapEntity: `(e) => ({ name: e.name, building: e.building, floor: e.floor, sensitive: e.sensitive, description: e.description ?? "" })`,
    detailFields: `
      <p><strong>Nom:</strong> {entity.name}</p>
      <p><strong>Bâtiment:</strong> {entity.building}</p>
      <p><strong>Étage:</strong> {entity.floor}</p>
      <p><strong>Sensible:</strong> {entity.sensitive ? "Oui" : "Non"}</p>
      <p><strong>Description:</strong> {entity.description || "—"}</p>
    `,
    typeImport: "Zone, ZoneRequest",
  },
];

for (const m of modules) {
  w(
    `features/${m.folder}/${m.entity}sListPage.tsx`.replace("DepartmentsListPage", "DepartmentsListPage").replace("ZonesListPage", "ZonesListPage"),
    listPage(m),
  );
  // fix names
}

function listPage(m) {
  const listName = m.folder === "departments" ? "DepartmentsListPage" : "ZonesListPage";
  const formName = m.folder === "departments" ? "DepartmentFormPage" : "ZoneFormPage";
  const detailName = m.folder === "departments" ? "DepartmentDetailPage" : "ZoneDetailPage";
  // overwritten below with correct filenames
  return "";
}

// Write departments properly
function writeCrud(opts) {
  const {
    listFile, formFile, detailFile, listExport, formExport, detailExport,
    api, basePath, title, singular, entityType, requestType, columns, formFields, schema, defaults, mapEntity, detailFields, extraImports = "",
  } = opts;

  w(`features/${opts.folder}/${listFile}`, `
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Plus } from "lucide-react";
import { ${api} } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/Feedback";
import { StatusBadge } from "@/components/ui/Card";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { ${entityType} } from "@/types/api";
${extraImports}

export function ${listExport}() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("id,desc");
  const [toDelete, setToDelete] = useState<${entityType} | null>(null);
  const keyword = useDebouncedValue(search);

  const query = useQuery({
    queryKey: ["${opts.folder}", page, keyword, sort],
    queryFn: () => ${api}.list({ page, size: 10, keyword, sort }),
  });

  const remove = useMutation({
    mutationFn: (id: number) => ${api}.remove(id),
    onSuccess: async () => {
      toast.success("${title.slice(0, -1)} supprimé");
      setToDelete(null);
      await qc.invalidateQueries({ queryKey: ["${opts.folder}"] });
    },
  });

  const columns = useMemo<Column<${entityType}>[]>(() => [
    ${columns}
    {
      key: "actions",
      header: "Actions",
      render: (r) => (
        <div className="flex gap-2">
          <Link className="text-brand-700 hover:underline" to={\`${basePath}/\${r.id}\`}>Détail</Link>
          <Link className="text-brand-700 hover:underline" to={\`${basePath}/\${r.id}/edit\`}>Éditer</Link>
          <button type="button" className="text-[var(--color-danger)] hover:underline" onClick={() => setToDelete(r)}>Supprimer</button>
        </div>
      ),
    },
  ], []);

  return (
    <div>
      <PageHeader
        title="${title}"
        description="Gestion des ${singular}s"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "${title}" }]}
        actions={<Link to="${basePath}/new"><Button><Plus className="size-4" /> Nouveau</Button></Link>}
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
        exportFileName="${opts.folder}.csv"
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Confirmer la suppression"
        description={\`Supprimer ${singular} \${toDelete ? ("name" in toDelete ? (toDelete as {name?: string}).name : toDelete.id) : ""} ?\`}
        danger
        loading={remove.isPending}
        onClose={() => setToDelete(null)}
        onConfirm={() => toDelete && remove.mutate(toDelete.id)}
      />
    </div>
  );
}
`);

  w(`features/${opts.folder}/${formFile}`, `
import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { ${api} } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { Checkbox, FieldError, Input, Label, Select, Textarea } from "@/components/ui/FormControls";
import { PageLoader } from "@/components/ui/Feedback";
import type { ${requestType} } from "@/types/api";
${extraImports}

const schema = ${schema};
type FormValues = z.infer<typeof schema>;

export function ${formExport}() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const entityQuery = useQuery({
    queryKey: ["${opts.folder}", id],
    queryFn: () => ${api}.get(Number(id)),
    enabled: isEdit,
  });

  const { register, handleSubmit, reset, setValue, watch, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: ${defaults},
  });

  useEffect(() => {
    if (entityQuery.data) reset(${mapEntity}(entityQuery.data));
  }, [entityQuery.data, reset]);

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      isEdit ? ${api}.update(Number(id), values as ${requestType}) : ${api}.create(values as ${requestType}),
    onSuccess: async (saved) => {
      toast.success(isEdit ? "Mis à jour" : "Créé");
      await qc.invalidateQueries({ queryKey: ["${opts.folder}"] });
      navigate(\`${basePath}/\${saved.id}\`);
    },
  });

  if (isEdit && entityQuery.isLoading) return <PageLoader />;

  return (
    <div>
      <PageHeader
        title={isEdit ? "Modifier ${singular}" : "Nouveau ${singular}"}
        breadcrumbs={[{ label: "${title}", to: "${basePath}" }, { label: isEdit ? "Édition" : "Création" }]}
      />
      <Card>
        <CardBody>
          <form className="grid max-w-2xl gap-4" onSubmit={handleSubmit((v) => save.mutate(v))}>
            ${formFields}
            <div className="flex gap-2">
              <Button type="submit" disabled={save.isPending}>{save.isPending ? "Enregistrement…" : "Enregistrer"}</Button>
              <Button type="button" variant="secondary" onClick={() => navigate("${basePath}")}>Annuler</Button>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
`);

  w(`features/${opts.folder}/${detailFile}`, `
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import toast from "react-hot-toast";
import { ${api} } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { ConfirmDialog, PageLoader } from "@/components/ui/Feedback";
import { formatDate } from "@/utils/cn";

export function ${detailExport}() {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const query = useQuery({ queryKey: ["${opts.folder}", id], queryFn: () => ${api}.get(Number(id)) });
  const remove = useMutation({
    mutationFn: () => ${api}.remove(Number(id)),
    onSuccess: async () => {
      toast.success("Supprimé");
      await qc.invalidateQueries({ queryKey: ["${opts.folder}"] });
      navigate("${basePath}");
    },
  });

  if (query.isLoading) return <PageLoader />;
  const entity = query.data;
  if (!entity) return <p>Introuvable</p>;

  return (
    <div>
      <PageHeader
        title={"${title.slice(0, -1)} #" + entity.id}
        breadcrumbs={[{ label: "${title}", to: "${basePath}" }, { label: "Détail" }]}
        actions={
          <>
            <Link to={\`${basePath}/\${entity.id}/edit\`}><Button variant="secondary">Éditer</Button></Link>
            <Button variant="danger" onClick={() => setOpen(true)}>Supprimer</Button>
          </>
        }
      />
      <Card>
        <CardBody className="space-y-2 text-sm">
          ${detailFields}
          <p><strong>Créé:</strong> {formatDate(entity.createdAt)}</p>
          <p><strong>Mis à jour:</strong> {formatDate(entity.updatedAt)}</p>
        </CardBody>
      </Card>
      <ConfirmDialog open={open} title="Supprimer ?" description="Action irréversible." danger loading={remove.isPending} onClose={() => setOpen(false)} onConfirm={() => remove.mutate()} />
    </div>
  );
}
`);
}

writeCrud({
  folder: "departments",
  listFile: "DepartmentsListPage.tsx",
  formFile: "DepartmentFormPage.tsx",
  detailFile: "DepartmentDetailPage.tsx",
  listExport: "DepartmentsListPage",
  formExport: "DepartmentFormPage",
  detailExport: "DepartmentDetailPage",
  api: "departmentsApi",
  basePath: "/departments",
  title: "Départements",
  singular: "département",
  entityType: "Department",
  requestType: "DepartmentRequest",
  columns: `{ key: "code", header: "Code", sortable: true, render: (r) => r.code, exportValue: (r) => r.code },
    { key: "name", header: "Nom", sortable: true, render: (r) => r.name, exportValue: (r) => r.name },
    { key: "active", header: "Actif", render: (r) => <StatusBadge tone={r.active ? "success" : "neutral"}>{r.active ? "Oui" : "Non"}</StatusBadge>, exportValue: (r) => (r.active ? "Oui" : "Non") },`,
  formFields: `<div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
            <div><Label>Code</Label><Input {...register("code")} /><FieldError message={errors.code?.message} /></div>
            <div><Label>Description</Label><Textarea {...register("description")} /></div>`,
  schema: `z.object({ name: z.string().min(2), code: z.string().min(2), description: z.string().optional() })`,
  defaults: `{ name: "", code: "", description: "" }`,
  mapEntity: `(e) => ({ name: e.name, code: e.code, description: e.description ?? "" })`,
  detailFields: `<p><strong>Code:</strong> {entity.code}</p>
          <p><strong>Nom:</strong> {entity.name}</p>
          <p><strong>Description:</strong> {entity.description || "—"}</p>
          <p><strong>Actif:</strong> {entity.active ? "Oui" : "Non"}</p>`,
});

writeCrud({
  folder: "zones",
  listFile: "ZonesListPage.tsx",
  formFile: "ZoneFormPage.tsx",
  detailFile: "ZoneDetailPage.tsx",
  listExport: "ZonesListPage",
  formExport: "ZoneFormPage",
  detailExport: "ZoneDetailPage",
  api: "zonesApi",
  basePath: "/zones",
  title: "Zones",
  singular: "zone",
  entityType: "Zone",
  requestType: "ZoneRequest",
  columns: `{ key: "name", header: "Nom", sortable: true, render: (r) => r.name, exportValue: (r) => r.name },
    { key: "building", header: "Bâtiment", render: (r) => r.building, exportValue: (r) => r.building },
    { key: "floor", header: "Étage", render: (r) => r.floor, exportValue: (r) => r.floor },
    { key: "sensitive", header: "Sensible", render: (r) => <StatusBadge tone={r.sensitive ? "danger" : "neutral"}>{r.sensitive ? "Oui" : "Non"}</StatusBadge>, exportValue: (r) => (r.sensitive ? "Oui" : "Non") },`,
  formFields: `<div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
            <div><Label>Bâtiment</Label><Input {...register("building")} /><FieldError message={errors.building?.message} /></div>
            <div><Label>Étage</Label><Input {...register("floor")} /><FieldError message={errors.floor?.message} /></div>
            <div><Label>Description</Label><Textarea {...register("description")} /></div>
            <Checkbox label="Zone sensible" checked={!!watch("sensitive")} onChange={(e) => setValue("sensitive", e.target.checked)} />`,
  schema: `z.object({ name: z.string().min(2), building: z.string().min(1), floor: z.string().min(1), sensitive: z.boolean(), description: z.string().optional() })`,
  defaults: `{ name: "", building: "", floor: "", sensitive: false, description: "" }`,
  mapEntity: `(e) => ({ name: e.name, building: e.building, floor: e.floor, sensitive: e.sensitive, description: e.description ?? "" })`,
  detailFields: `<p><strong>Nom:</strong> {entity.name}</p>
          <p><strong>Bâtiment:</strong> {entity.building}</p>
          <p><strong>Étage:</strong> {entity.floor}</p>
          <p><strong>Sensible:</strong> {entity.sensitive ? "Oui" : "Non"}</p>
          <p><strong>Description:</strong> {entity.description || "—"}</p>`,
});

console.log("generated departments + zones + hook");
