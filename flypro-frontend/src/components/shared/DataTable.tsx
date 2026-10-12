import type { ReactNode } from "react";
import { Columns3, Download, Search } from "lucide-react";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/FormControls";
import { EmptyState, Skeleton } from "@/components/ui/Feedback";
import { downloadCsv } from "@/utils/cn";
import { cn } from "@/utils/cn";

export type Column<T> = {
  key: string;
  header: string;
  sortable?: boolean;
  render: (row: T) => ReactNode;
  exportValue?: (row: T) => string | number | null | undefined;
};

type DataTableProps<T> = {
  columns: Column<T>[];
  rows: T[];
  loading?: boolean;
  search: string;
  onSearchChange: (value: string) => void;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  sort?: string;
  onSortChange?: (sort: string) => void;
  exportFileName?: string;
  emptyTitle?: string;
};

export function DataTable<T extends { id?: number | string }>({
  columns,
  rows,
  loading,
  search,
  onSearchChange,
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  sort,
  onSortChange,
  exportFileName = "export.csv",
  emptyTitle = "Aucune donnée",
}: DataTableProps<T>) {
  const [visible, setVisible] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(columns.map((c) => [c.key, true])),
  );
  const [colsOpen, setColsOpen] = useState(false);

  const activeColumns = useMemo(
    () => columns.filter((c) => visible[c.key] !== false),
    [columns, visible],
  );

  const exportRows = rows.map((row) => {
    const obj: Record<string, unknown> = {};
    activeColumns.forEach((col) => {
      obj[col.header] = col.exportValue ? col.exportValue(row) : String(col.render(row) ?? "");
    });
    return obj;
  });

  return (
    <div className="overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)]">
      <div className="flex flex-col gap-3 border-b border-[var(--color-border)] p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full max-w-sm">
          <Search className="pointer-events-none absolute top-2.5 left-3 size-4 text-ink-400" aria-hidden />
          <Input
            className="pl-9"
            placeholder="Recherche…"
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            aria-label="Recherche dans le tableau"
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <div className="relative">
            <Button variant="secondary" type="button" size="sm" onClick={() => setColsOpen((v) => !v)}>
              <Columns3 className="size-4" />
              Colonnes
            </Button>
            {colsOpen ? (
              <div className="absolute right-0 z-20 mt-2 w-52 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-2 shadow-xl">
                {columns.map((col) => (
                  <label key={col.key} className="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-[var(--color-surface-muted)]">
                    <input
                      type="checkbox"
                      checked={visible[col.key] !== false}
                      onChange={(e) => setVisible((v) => ({ ...v, [col.key]: e.target.checked }))}
                    />
                    {col.header}
                  </label>
                ))}
              </div>
            ) : null}
          </div>
          <Button
            variant="secondary"
            type="button"
            size="sm"
            onClick={() => downloadCsv(exportFileName, exportRows)}
            disabled={!rows.length}
          >
            <Download className="size-4" />
            Export CSV
          </Button>
        </div>
      </div>

      <div className="max-h-[640px] overflow-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="sticky top-0 z-10 bg-[var(--color-surface-muted)]/95 text-[var(--color-ink-500)] backdrop-blur">
            <tr>
              {activeColumns.map((col) => (
                <th key={col.key} className="border-b border-[var(--color-border)] px-4 py-3 font-semibold whitespace-nowrap">
                  {col.sortable && onSortChange ? (
                    <button
                      type="button"
                      className="hover:text-navy-900 dark:hover:text-white"
                      onClick={() => {
                        const desc = sort === `${col.key},asc`;
                        onSortChange(`${col.key},${desc ? "desc" : "asc"}`);
                      }}
                    >
                      {col.header}
                    </button>
                  ) : (
                    col.header
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <tr key={i}>
                  <td colSpan={activeColumns.length} className="px-4 py-3">
                    <Skeleton className="h-6 w-full" />
                  </td>
                </tr>
              ))
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={activeColumns.length} className="p-0">
                  <EmptyState title={emptyTitle} description="Ajustez vos filtres ou créez un nouvel élément." />
                </td>
              </tr>
            ) : (
              rows.map((row, idx) => (
                <tr
                  key={row.id ?? idx}
                  className={cn(
                    "border-t border-[var(--color-border)] transition-colors hover:bg-brand-50/60",
                  )}
                >
                  {activeColumns.map((col) => (
                    <td key={col.key} className="px-4 py-3 align-middle whitespace-nowrap">
                      {col.render(row)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between gap-3 border-t border-[var(--color-border)] px-4 py-3 text-sm">
        <span className="text-ink-500">
          {totalElements} résultat(s) · page {page + 1}/{Math.max(totalPages, 1)} · {size}/page
        </span>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
            Précédent
          </Button>
          <Button
            variant="secondary"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => onPageChange(page + 1)}
          >
            Suivant
          </Button>
        </div>
      </div>
    </div>
  );
}
