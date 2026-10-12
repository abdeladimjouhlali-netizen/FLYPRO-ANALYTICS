import type { InputHTMLAttributes, TextareaHTMLAttributes, SelectHTMLAttributes, ReactNode } from "react";
import { cn } from "@/utils/cn";

const fieldClass =
  "w-full rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink-900)] outline-none focus:ring-2 focus:ring-brand-500";

export function Label({ children, htmlFor }: { children: ReactNode; htmlFor?: string }) {
  return (
    <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-medium text-ink-700">
      {children}
    </label>
  );
}

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(fieldClass, className)} {...props} />;
}

export function Textarea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn(fieldClass, "min-h-24", className)} {...props} />;
}

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cn(fieldClass, className)} {...props}>
      {children}
    </select>
  );
}

/** Floating label input for modern enterprise forms */
export function FloatingInput({
  label,
  id,
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  const inputId = id ?? props.name ?? label;
  return (
    <div className="relative">
      <input
        id={inputId}
        placeholder=" "
        className={cn(fieldClass, "peer pt-5 pb-2", className)}
        {...props}
      />
      <label
        htmlFor={inputId}
        className="pointer-events-none absolute top-2 left-3 text-[10px] font-semibold tracking-wide text-ink-400 uppercase transition-all peer-placeholder-shown:top-1/2 peer-placeholder-shown:-translate-y-1/2 peer-placeholder-shown:text-sm peer-placeholder-shown:normal-case peer-placeholder-shown:tracking-normal peer-placeholder-shown:font-normal peer-focus:top-2 peer-focus:translate-y-0 peer-focus:text-[10px] peer-focus:font-semibold peer-focus:tracking-wide peer-focus:text-brand-600 peer-focus:uppercase"
      >
        {label}
      </label>
    </div>
  );
}

export function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="mt-1 text-xs text-[var(--color-danger)]">{message}</p>;
}

export function Checkbox({
  label,
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className={cn("inline-flex items-center gap-2 text-sm", className)}>
      <input type="checkbox" className="size-4 rounded accent-brand-600" {...props} />
      {label}
    </label>
  );
}

export function Switch({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: (value: boolean) => void;
  label: string;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="inline-flex items-center gap-3 text-sm"
    >
      <span
        className={cn(
          "relative h-6 w-11 rounded-full transition",
          checked ? "bg-brand-600" : "bg-ink-300",
        )}
      >
        <span
          className={cn(
            "absolute top-0.5 left-0.5 size-5 rounded-full bg-white shadow transition",
            checked && "translate-x-5",
          )}
        />
      </span>
      {label}
    </button>
  );
}
