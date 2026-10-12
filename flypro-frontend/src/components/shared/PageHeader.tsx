import { Link } from "react-router-dom";
import type { ReactNode } from "react";

export function PageHeader({
  title,
  description,
  breadcrumbs,
  actions,
  preserveCase,
}: {
  title: string;
  description?: string;
  breadcrumbs?: { label: string; to?: string }[];
  actions?: ReactNode;
  /** Garde la casse du titre (désactive le lowercase du style logo) */
  preserveCase?: boolean;
}) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {breadcrumbs?.length ? (
          <nav className="mb-2 flex flex-wrap items-center gap-1 text-xs text-white/70">
            {breadcrumbs.map((b, i) => (
              <span key={`${b.label}-${i}`} className="inline-flex items-center gap-1">
                {i > 0 ? <span>/</span> : null}
                {b.to ? (
                  <Link to={b.to} className="hover:text-white">
                    {b.label}
                  </Link>
                ) : (
                  <span className="text-white/90">{b.label}</span>
                )}
              </span>
            ))}
          </nav>
        ) : null}
        <h1
          className={`ram-wordmark text-3xl text-brand-600 drop-shadow-sm sm:text-4xl${preserveCase ? " !normal-case" : ""}`}
        >
          {title}
        </h1>
        {description ? <p className="mt-1 text-sm text-white/80 drop-shadow">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
    </div>
  );
}

export function SectionTitle({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-4 flex items-end justify-between gap-3">
      <div>
        <h2 className="ram-wordmark text-lg text-brand-600">{title}</h2>
        {description ? <p className="mt-0.5 text-sm text-white/75">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}
