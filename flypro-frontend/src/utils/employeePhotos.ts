const STORAGE_PREFIX = "flypro.employeePhoto.";

function keyFor(employeeId?: number | null, employeeNumber?: string | null): string | null {
  if (employeeId != null) return `${STORAGE_PREFIX}id:${employeeId}`;
  if (employeeNumber) return `${STORAGE_PREFIX}num:${employeeNumber}`;
  return null;
}

/** Photos employé stockées côté navigateur (aucun endpoint d'upload côté backend). */
export function getEmployeePhoto(
  employeeId?: number | null,
  employeeNumber?: string | null,
): string | undefined {
  const key = keyFor(employeeId, employeeNumber);
  if (!key) return undefined;
  try {
    return localStorage.getItem(key) ?? undefined;
  } catch {
    return undefined;
  }
}

export function setEmployeePhoto(
  dataUrl: string,
  employeeId?: number | null,
  employeeNumber?: string | null,
): void {
  const key = keyFor(employeeId, employeeNumber);
  if (!key) return;
  try {
    localStorage.setItem(key, dataUrl);
  } catch {
    // quota dépassé ou storage indisponible : la photo reste en mémoire pour la session
  }
}

export function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}
