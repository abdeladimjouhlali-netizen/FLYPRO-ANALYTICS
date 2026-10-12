import { useMemo, useState, useCallback, type DragEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { FileUp, Upload } from "lucide-react";
import { importsApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { PageLoader } from "@/components/ui/Feedback";
import type { ImportBatch } from "@/types/api";
import { cn, formatDate } from "@/utils/cn";

export function ImportsListPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const query = useQuery({ queryKey: ["imports", page], queryFn: () => importsApi.list({ page, size: 10 }) });
  const columns = useMemo<Column<ImportBatch>[]>(
    () => [
      { key: "fileName", header: "Fichier", render: (r) => r.fileName, exportValue: (r) => r.fileName },
      { key: "fileType", header: "Type", render: (r) => r.fileType, exportValue: (r) => r.fileType },
      { key: "status", header: "Statut", render: (r) => <StatusBadge>{r.status}</StatusBadge>, exportValue: (r) => r.status },
      { key: "total", header: "Total", render: (r) => r.totalRecords, exportValue: (r) => r.totalRecords },
      { key: "invalid", header: "Invalides", render: (r) => r.invalidRecords, exportValue: (r) => r.invalidRecords },
      { key: "createdAt", header: "Créé", render: (r) => formatDate(r.createdAt), exportValue: (r) => r.createdAt },
      {
        key: "actions",
        header: "Actions",
        render: (r) => (
          <Link className="font-semibold text-brand-600 hover:underline" to={`/imports/${r.id}`}>
            Rapport
          </Link>
        ),
      },
    ],
    [],
  );
  return (
    <div>
      <PageHeader
        title="Imports"
        description="Historique et validation des lots d’import"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Imports" }]}
        actions={
          <Link to="/imports/upload">
            <Button>
              <Upload className="size-4" /> Nouvel import
            </Button>
          </Link>
        }
      />
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
        exportFileName="imports.csv"
      />
    </div>
  );
}

export function ImportUploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportBatch | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const qc = useQueryClient();

  const onDrop = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const f = e.dataTransfer.files?.[0];
    if (f) setFile(f);
  }, []);

  const previewMutation = useMutation({
    mutationFn: () => {
      if (!file) throw new Error("Fichier requis");
      return importsApi.preview(file);
    },
    onSuccess: (data) => {
      setPreview(data);
      toast.success("Prévisualisation prête");
    },
  });
  const executeMutation = useMutation({
    mutationFn: () => {
      if (!preview) throw new Error("Prévisualisation requise");
      return importsApi.execute(preview.id);
    },
    onSuccess: async (data) => {
      toast.success("Import exécuté");
      await qc.invalidateQueries({ queryKey: ["imports"] });
      setPreview(data);
    },
  });

  const progress =
    preview && preview.totalRecords > 0
      ? Math.round(((preview.validRecords + preview.invalidRecords) / preview.totalRecords) * 100)
      : previewMutation.isPending
        ? 45
        : executeMutation.isPending
          ? 75
          : 0;

  return (
    <div>
      <PageHeader
        title="Upload d'import"
        description="Glissez-déposez un fichier, validez, puis exécutez"
        breadcrumbs={[{ label: "Imports", to: "/imports" }, { label: "Upload" }]}
      />
      <Card>
        <CardHeader
          title="Fichier CSV / Excel / JSON"
          description="Prévisualisez avant exécution pour contrôler les erreurs de validation."
        />
        <CardBody className="space-y-5">
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={onDrop}
            className={cn(
              "flex flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-14 text-center transition",
              dragOver
                ? "border-sky-400 bg-sky-50 dark:bg-sky-400/10"
                : "border-[var(--color-border)] bg-[var(--color-surface-muted)]/60",
            )}
          >
            <FileUp className="size-10 text-ink-400" />
            <p className="mt-3 text-sm font-semibold text-navy-900 dark:text-white">
              Glissez votre fichier ici
            </p>
            <p className="mt-1 text-xs text-ink-500">ou sélectionnez depuis votre poste</p>
            <label className="mt-4">
              <span className="inline-flex cursor-pointer items-center rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700">
                Parcourir
              </span>
              <input
                type="file"
                accept=".csv,.xlsx,.json"
                className="sr-only"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </label>
            {file ? (
              <p className="mt-3 text-sm text-brand-600">
                {file.name} · {(file.size / 1024).toFixed(1)} Ko
              </p>
            ) : null}
          </div>

          {(previewMutation.isPending || executeMutation.isPending || progress > 0) && (
            <div>
              <div className="mb-1 flex justify-between text-xs font-medium text-ink-500">
                <span>Progression</span>
                <span>{progress}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-navy-800">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-brand-600 to-sky-400 transition-all duration-500"
                  style={{ width: `${Math.max(progress, previewMutation.isPending ? 30 : 0)}%` }}
                />
              </div>
            </div>
          )}

          <div className="flex flex-wrap gap-2">
            <Button disabled={!file || previewMutation.isPending} onClick={() => previewMutation.mutate()}>
              Prévisualiser
            </Button>
            <Button
              variant="secondary"
              disabled={!preview || executeMutation.isPending}
              onClick={() => executeMutation.mutate()}
            >
              Exécuter l&apos;import
            </Button>
          </div>

          {preview ? (
            <div className="rounded-xl border border-[var(--color-border)] p-4 text-sm">
              <p className="font-semibold text-navy-900 dark:text-white">Résumé de validation</p>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                <p>
                  <strong>Batch:</strong> #{preview.id}
                </p>
                <p>
                  <strong>Statut:</strong> <StatusBadge>{preview.status}</StatusBadge>
                </p>
                <p>
                  <strong>Total:</strong> {preview.totalRecords}
                </p>
                <p>
                  <strong>Valides:</strong> {preview.validRecords} · Invalides: {preview.invalidRecords}
                </p>
              </div>
              {(preview.issues?.length ?? 0) > 0 ? (
                <div className="mt-3">
                  <p className="font-semibold text-danger">Erreurs de validation</p>
                  <ul className="mt-2 max-h-48 space-y-1 overflow-auto text-xs text-danger">
                    {preview.issues?.map((issue) => (
                      <li key={issue.id}>
                        Ligne {issue.lineNumber ?? "?"} · {issue.fieldName}: {issue.message}
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <Link className="mt-3 inline-block font-semibold text-brand-600 hover:underline" to={`/imports/${preview.id}`}>
                Voir le rapport complet
              </Link>
            </div>
          ) : null}
        </CardBody>
      </Card>
    </div>
  );
}

export function ImportDetailPage() {
  const { id } = useParams();
  const query = useQuery({ queryKey: ["imports", id], queryFn: () => importsApi.get(Number(id)) });
  const report = useQuery({ queryKey: ["imports", id, "report"], queryFn: () => importsApi.report(Number(id)) });
  if (query.isLoading) return <PageLoader />;
  const batch = query.data;
  if (!batch) return <p>Introuvable</p>;
  return (
    <div>
      <PageHeader title={batch.fileName} breadcrumbs={[{ label: "Imports", to: "/imports" }, { label: `#${batch.id}` }]} />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardBody className="space-y-2 text-sm">
            <p>
              <strong>Type:</strong> {batch.fileType}
            </p>
            <p>
              <strong>Statut:</strong> <StatusBadge>{batch.status}</StatusBadge>
            </p>
            <p>
              <strong>Total:</strong> {batch.totalRecords}
            </p>
            <p>
              <strong>Valides:</strong> {batch.validRecords}
            </p>
            <p>
              <strong>Invalides:</strong> {batch.invalidRecords}
            </p>
            <p>
              <strong>Doublons:</strong> {batch.duplicateRecords}
            </p>
            <p>
              <strong>Créé:</strong> {formatDate(batch.createdAt)}
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardHeader title="Rapport" />
          <CardBody>
            <pre className="max-h-80 overflow-auto rounded-xl bg-[var(--color-surface-muted)] p-3 text-xs">
              {JSON.stringify(report.data ?? {}, null, 2)}
            </pre>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
