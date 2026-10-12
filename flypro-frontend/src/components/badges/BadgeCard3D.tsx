import { useEffect, useId, useRef, useState } from "react";
import { Camera, UserRound } from "lucide-react";
import type { BadgeStatus, BadgeType } from "@/types/api";
import {
  fileToDataUrl,
  getEmployeePhoto,
  setEmployeePhoto,
} from "@/utils/employeePhotos";
import { cn } from "@/utils/cn";

export type BadgeCardData = {
  badgeNumber?: string;
  type?: BadgeType | string;
  status?: BadgeStatus | string;
  issueDate?: string;
  expirationDate?: string;
  employeeId?: number;
  employeeFirstName?: string;
  employeeLastName?: string;
  employeeNumber?: string;
  photoUrl?: string;
};

const statusLabel: Record<string, string> = {
  ACTIVE: "Actif",
  SUSPENDED: "Suspendu",
  EXPIRED: "Expiré",
  REVOKED: "Révoqué",
  LOST: "Perdu",
  PENDING_ACTIVATION: "En attente",
};

function formatShortDate(value?: string) {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleDateString("fr-MA", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  } catch {
    return value;
  }
}

export function BadgeCard3D({
  data,
  className,
  interactive = true,
  allowPhotoUpload = true,
  onPhotoChange,
}: {
  data: BadgeCardData;
  className?: string;
  interactive?: boolean;
  allowPhotoUpload?: boolean;
  onPhotoChange?: (dataUrl: string) => void;
}) {
  const [flipped, setFlipped] = useState(false);
  const [photo, setPhoto] = useState<string | undefined>(data.photoUrl);
  const inputRef = useRef<HTMLInputElement>(null);
  const inputId = useId();

  useEffect(() => {
    if (data.photoUrl) {
      setPhoto(data.photoUrl);
      return;
    }
    setPhoto(getEmployeePhoto(data.employeeId, data.employeeNumber));
  }, [data.photoUrl, data.employeeId, data.employeeNumber]);

  const first = data.employeeFirstName?.trim() || "";
  const last = data.employeeLastName?.trim() || "";
  const fullName = [first, last].filter(Boolean).join(" ") || "Nom prénom";
  const number = data.badgeNumber?.trim() || "RAM-0000";
  const type = data.type || "RFID";
  const status = data.status || "PENDING_ACTIVATION";
  const canPersist = data.employeeId != null || Boolean(data.employeeNumber);

  async function handlePhotoSelected(file: File | undefined) {
    if (!file || !file.type.startsWith("image/")) return;
    const dataUrl = await fileToDataUrl(file);
    setPhoto(dataUrl);
    if (canPersist) {
      setEmployeePhoto(dataUrl, data.employeeId, data.employeeNumber);
    }
    onPhotoChange?.(dataUrl);
  }

  return (
    <div className={cn("perspective-[1200px]", className)}>
      <div
        role={interactive ? "button" : undefined}
        tabIndex={interactive ? 0 : undefined}
        aria-label={flipped ? "Retourner la carte (recto)" : "Retourner la carte (verso)"}
        onClick={() => interactive && setFlipped((v) => !v)}
        onKeyDown={(e) => {
          if (!interactive) return;
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            setFlipped((v) => !v);
          }
        }}
        className={cn(
          "relative mx-auto block h-[340px] w-full max-w-[220px] origin-center transition-transform duration-500 ease-out [transform-style:preserve-3d]",
          interactive && "cursor-pointer hover:-translate-y-1",
          !interactive && "cursor-default",
        )}
        style={{
          transform: flipped
            ? "rotateY(180deg) rotateX(6deg)"
            : "rotateY(-18deg) rotateX(8deg)",
        }}
      >
        {/* Recto */}
        <div className="absolute inset-0 overflow-hidden rounded-2xl border border-white/20 bg-gradient-to-br from-brand-800 via-brand-600 to-brand-900 text-white shadow-[0_25px_50px_-20px_rgba(28,24,23,0.65),0_0_0_1px_rgba(255,255,255,0.08)] [backface-visibility:hidden]">
          <div
            className="pointer-events-none absolute inset-0 opacity-30"
            style={{
              backgroundImage:
                "repeating-linear-gradient(-18deg, transparent, transparent 12px, rgba(255,255,255,0.06) 12px, rgba(255,255,255,0.06) 13px)",
            }}
          />
          <div className="relative flex h-full flex-col p-4">
            <div className="flex items-start justify-between gap-2">
              <img
                src="/brand/royal-air-maroc-logo.png"
                alt=""
                className="h-10 w-auto rounded bg-white/95 px-1.5 py-1 object-contain"
                decoding="async"
              />
              <span className="rounded-md bg-white/15 px-2 py-0.5 text-[9px] font-bold tracking-[0.14em] uppercase backdrop-blur-sm">
                {type}
              </span>
            </div>

            <div className="mt-5 flex items-center gap-3">
              <div className="relative size-16 shrink-0 overflow-hidden rounded-xl ring-1 ring-white/25">
                {photo ? (
                  <img
                    src={photo}
                    alt={fullName}
                    className="size-full object-cover"
                    decoding="async"
                  />
                ) : (
                  <div className="flex size-full flex-col items-center justify-center gap-0.5 bg-white/15 text-white/90 backdrop-blur-sm">
                    <UserRound className="size-6" aria-hidden />
                  </div>
                )}
                {allowPhotoUpload ? (
                  <label
                    htmlFor={inputId}
                    className="absolute inset-0 flex cursor-pointer items-center justify-center bg-black/45 opacity-0 transition hover:opacity-100"
                    onClick={(e) => e.stopPropagation()}
                    title="Ajouter une photo"
                  >
                    <Camera className="size-5 text-white" />
                    <span className="sr-only">Ajouter une photo employé</span>
                  </label>
                ) : null}
              </div>
              <div className="min-w-0 text-left">
                <p className="truncate text-[10px] font-semibold tracking-[0.16em] text-white/70 uppercase">
                  Employé
                </p>
                <p className="truncate text-sm font-bold leading-tight">{fullName}</p>
                <p className="truncate text-[11px] text-white/75">
                  {data.employeeNumber || "N° employé"}
                </p>
              </div>
            </div>

            <div className="mt-auto space-y-2 rounded-xl bg-black/20 p-3 backdrop-blur-sm">
              <div>
                <p className="text-[9px] font-semibold tracking-[0.14em] text-white/60 uppercase">
                  N° badge
                </p>
                <p className="font-mono text-base font-bold tracking-wider">{number}</p>
              </div>
              <div className="flex items-center justify-between gap-2 text-[10px]">
                <span className="rounded-full bg-white/15 px-2 py-0.5 font-semibold">
                  {statusLabel[status] ?? status}
                </span>
                <span className="text-white/70">Exp. {formatShortDate(data.expirationDate)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Verso */}
        <div className="absolute inset-0 overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-ink-900)] shadow-[0_25px_50px_-20px_rgba(28,24,23,0.65)] [backface-visibility:hidden] [transform:rotateY(180deg)]">
          <div className="flex h-full flex-col p-4">
            <div className="h-10 rounded bg-[#1c1817]" aria-hidden />
            <div className="mt-4 space-y-3 text-left text-xs">
              <p className="ram-wordmark text-sm text-brand-600">royal air maroc</p>
              <div>
                <p className="text-[10px] font-semibold tracking-wide text-[var(--color-ink-500)] uppercase">
                  Émission
                </p>
                <p className="font-semibold">{formatShortDate(data.issueDate)}</p>
              </div>
              <div>
                <p className="text-[10px] font-semibold tracking-wide text-[var(--color-ink-500)] uppercase">
                  Expiration
                </p>
                <p className="font-semibold">{formatShortDate(data.expirationDate)}</p>
              </div>
              <div>
                <p className="text-[10px] font-semibold tracking-wide text-[var(--color-ink-500)] uppercase">
                  Technologie
                </p>
                <p className="font-semibold">{type}</p>
              </div>
            </div>
            <div className="mt-auto">
              <div className="mx-auto size-16 overflow-hidden rounded-xl ring-1 ring-[var(--color-border)]">
                {photo ? (
                  <img src={photo} alt="" className="size-full object-cover" decoding="async" />
                ) : (
                  <div className="flex size-full items-center justify-center bg-[var(--color-surface-muted)]">
                    <UserRound className="size-6 text-[var(--color-ink-400)]" />
                  </div>
                )}
              </div>
              <p className="mt-2 text-center font-mono text-[10px] text-[var(--color-ink-500)]">
                {number}
              </p>
              {interactive ? (
                <p className="mt-1 text-center text-[9px] text-[var(--color-ink-400)]">
                  Cliquer pour retourner
                </p>
              ) : null}
            </div>
          </div>
        </div>
      </div>

      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept="image/*"
        className="sr-only"
        onChange={(e) => {
          void handlePhotoSelected(e.target.files?.[0]);
          e.target.value = "";
        }}
      />

      {allowPhotoUpload ? (
        <div className="mt-3 flex flex-col items-center gap-1">
          <button
            type="button"
            className="inline-flex items-center gap-1.5 rounded-md bg-white/90 px-3 py-1.5 text-xs font-semibold text-brand-700 shadow-sm hover:bg-white"
            onClick={() => inputRef.current?.click()}
          >
            <Camera className="size-3.5" />
            {photo ? "Changer la photo" : "Ajouter une photo"}
          </button>
          {interactive ? (
            <p className="text-center text-xs text-white/70">Cliquez sur la carte pour la retourner</p>
          ) : null}
        </div>
      ) : interactive ? (
        <p className="mt-3 text-center text-xs text-white/70">Cliquez sur la carte pour la retourner</p>
      ) : null}
    </div>
  );
}
