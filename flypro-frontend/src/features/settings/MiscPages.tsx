import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import toast from "react-hot-toast";
import { LogOut, UserPlus } from "lucide-react";
import { usersApi } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { PageLoader } from "@/components/ui/Feedback";
import { ROLE_LABELS, ROLES } from "@/constants/app";
import { useAuth } from "@/contexts/AuthContext";
import { useTheme } from "@/contexts/ThemeContext";
import { formatDate } from "@/utils/cn";
import { useNavigate } from "react-router-dom";

const createUserSchema = z.object({
  username: z.string().min(3, "Min. 3 caractères").max(50),
  email: z.string().email("Email invalide"),
  password: z.string().min(8, "Min. 8 caractères"),
  role: z.enum(["ROLE_ADMIN", "ROLE_ANALYST"]),
});

type CreateUserForm = z.infer<typeof createUserSchema>;

export function UsersPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const listQuery = useQuery({
    queryKey: ["users"],
    queryFn: () => usersApi.list({ page: 0, size: 100, sort: "username,asc" }),
  });

  const form = useForm<CreateUserForm>({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      username: "",
      email: "",
      password: "Admin@123",
      role: "ROLE_ADMIN",
    },
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateUserForm) => usersApi.create(payload),
    onSuccess: (created) => {
      toast.success(`Compte ${created.username} créé (${ROLE_LABELS[created.role]})`);
      form.reset({ username: "", email: "", password: "Admin@123", role: "ROLE_ADMIN" });
      void queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: (err: unknown) => {
      const msg =
        err && typeof err === "object" && "response" in err
          ? // eslint-disable-next-line @typescript-eslint/no-explicit-any
            ((err as any).response?.data?.message as string | undefined)
          : undefined;
      toast.error(msg ?? "Création impossible");
    },
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      usersApi.setEnabled(id, enabled),
    onSuccess: () => {
      toast.success("Statut mis à jour");
      void queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: () => toast.error("Action impossible"),
  });

  if (listQuery.isLoading) return <PageLoader />;

  const users = listQuery.data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Utilisateurs"
        description="Seul le Super Admin peut créer un Administrateur (ou un Analyste)."
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Utilisateurs" }]}
      />

      <div className="mb-3 rounded-xl border border-brand-200 bg-brand-50 px-4 py-3 text-sm text-[var(--color-ink-700)]">
        Connecté en Super Admin : <strong>{user?.username}</strong>. Les rôles Admin et Analyste
        n’ont pas accès à cette page.
      </div>

      <div className="grid gap-4 xl:grid-cols-[380px_1fr]">
        <Card>
          <CardHeader
            title="Créer un Admin"
            description="Compte console métier (ROLE_ADMIN)"
            action={<UserPlus className="size-4 text-brand-600" />}
          />
          <CardBody>
            <form
              className="space-y-3"
              onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
            >
              <div>
                <label className="mb-1 block text-xs font-semibold">Identifiant</label>
                <input
                  className="w-full rounded-lg border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
                  {...form.register("username")}
                  placeholder="ex. directeur.ops"
                />
                {form.formState.errors.username && (
                  <p className="mt-1 text-xs text-red-600">{form.formState.errors.username.message}</p>
                )}
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold">Email</label>
                <input
                  type="email"
                  className="w-full rounded-lg border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
                  {...form.register("email")}
                  placeholder="ex. directeur.ops@ram.ma"
                />
                {form.formState.errors.email && (
                  <p className="mt-1 text-xs text-red-600">{form.formState.errors.email.message}</p>
                )}
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold">Mot de passe</label>
                <input
                  type="text"
                  className="w-full rounded-lg border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
                  {...form.register("password")}
                />
                {form.formState.errors.password && (
                  <p className="mt-1 text-xs text-red-600">{form.formState.errors.password.message}</p>
                )}
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold">Rôle</label>
                <select
                  className="w-full rounded-lg border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
                  {...form.register("role")}
                >
                  <option value="ROLE_ADMIN">Administrateur</option>
                  <option value="ROLE_ANALYST">Analyste</option>
                </select>
                <p className="mt-1 text-[11px] text-[var(--color-ink-500)]">
                  Impossible de créer un autre Super Admin ici.
                </p>
              </div>
              <Button type="submit" disabled={createMutation.isPending} className="w-full">
                <UserPlus className="size-4" />
                {createMutation.isPending ? "Création…" : "Créer le compte"}
              </Button>
            </form>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Comptes existants" description={`${users.length} utilisateur(s)`} />
          <CardBody className="space-y-2">
            {users.length === 0 ? (
              <p className="text-sm text-[var(--color-ink-500)]">Aucun utilisateur</p>
            ) : (
              users.map((u) => (
                <div
                  key={u.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm"
                >
                  <div>
                    <p className="font-semibold">{u.username}</p>
                    <p className="text-xs text-[var(--color-ink-500)]">
                      {u.email} · {formatDate(u.createdAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge tone={u.role === "ROLE_SUPER_ADMIN" ? "brand" : "neutral"}>
                      {ROLE_LABELS[u.role]}
                    </StatusBadge>
                    <StatusBadge tone={u.enabled ? "success" : "danger"}>
                      {u.enabled ? "Actif" : "Inactif"}
                    </StatusBadge>
                    {u.role !== "ROLE_SUPER_ADMIN" && (
                      <Button
                        size="sm"
                        variant="secondary"
                        disabled={toggleMutation.isPending}
                        onClick={() =>
                          toggleMutation.mutate({ id: u.id, enabled: !u.enabled })
                        }
                      >
                        {u.enabled ? "Désactiver" : "Activer"}
                      </Button>
                    )}
                  </div>
                </div>
              ))
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}

export function RolesPage() {
  const matrix = [
    {
      role: ROLES.SUPER_ADMIN,
      access: "Accès complet + seul rôle autorisé à créer un Admin / Analyste",
    },
    { role: ROLES.ADMIN, access: "Lecture/écriture métier, imports, dashboard" },
    { role: ROLES.ANALYST, access: "Lecture dashboard, analytics, alertes" },
    { role: ROLES.EMPLOYEE, access: "Portail employé (créé automatiquement avec l’employé)" },
  ] as const;
  return (
    <div>
      <PageHeader
        title="Rôles"
        description="Matrice des rôles FlyPro — création Admin = Super Admin uniquement"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Rôles" }]}
      />
      <div className="grid gap-3">
        {matrix.map((item) => (
          <Card key={item.role}>
            <CardBody className="flex items-center justify-between gap-3">
              <div>
                <p className="font-semibold">{ROLE_LABELS[item.role]}</p>
                <p className="text-sm text-[var(--color-ink-500)]">{item.access}</p>
              </div>
              <StatusBadge>{item.role}</StatusBadge>
            </CardBody>
          </Card>
        ))}
      </div>
    </div>
  );
}

export function SettingsPage() {
  const { theme, setTheme } = useTheme();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    toast.success("Déconnexion réussie");
    navigate("/login", { replace: true });
  }

  return (
    <div>
      <PageHeader title="Paramètres" breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Paramètres" }]} />
      <Card>
        <CardHeader title="Apparence" description="Thème clair / sombre" />
        <CardBody className="flex gap-2">
          <button
            type="button"
            className={`rounded-md px-4 py-2 text-sm ${theme === "light" ? "bg-brand-600 text-white" : "bg-[var(--color-surface-muted)]"}`}
            onClick={() => setTheme("light")}
          >
            Clair
          </button>
          <button
            type="button"
            className={`rounded-md px-4 py-2 text-sm ${theme === "dark" ? "bg-brand-600 text-white" : "bg-[var(--color-surface-muted)]"}`}
            onClick={() => setTheme("dark")}
          >
            Sombre
          </button>
        </CardBody>
      </Card>
      <Card className="mt-4">
        <CardHeader title="API" />
        <CardBody className="space-y-1 text-sm">
          <p>Backend Spring : {import.meta.env.VITE_API_BASE_URL}</p>
          <p>Service AI : {import.meta.env.VITE_AI_BASE_URL}</p>
        </CardBody>
      </Card>
      <Card className="mt-4">
        <CardHeader
          title="Session"
          description={user ? `Connecté en tant que ${user.username}` : "Aucune session"}
        />
        <CardBody>
          <Button variant="danger" onClick={() => void handleLogout()}>
            <LogOut className="size-4" />
            Déconnexion
          </Button>
        </CardBody>
      </Card>
    </div>
  );
}

export function ProfilePage() {
  const { user } = useAuth();
  return (
    <div>
      <PageHeader title="Mon profil" breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Profil" }]} />
      <Card>
        <CardBody className="space-y-2 text-sm">
          <p>
            <strong>Nom d&apos;utilisateur:</strong> {user?.username}
          </p>
          <p>
            <strong>Email:</strong> {user?.email}
          </p>
          <p>
            <strong>Rôle:</strong> {user ? ROLE_LABELS[user.role] : "—"}
          </p>
          <p>
            <strong>Compte verrouillé:</strong> {user?.accountLocked ? "Oui" : "Non"}
          </p>
        </CardBody>
      </Card>
    </div>
  );
}
