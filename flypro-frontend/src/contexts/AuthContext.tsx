import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/api/services";
import { tokenStore } from "@/api/tokenStore";
import type { User } from "@/types/api";

type AuthContextValue = {
  user: User | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  refreshMe: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [bootstrapped, setBootstrapped] = useState(false);
  const hasToken = Boolean(tokenStore.getAccess());

  const meQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: authApi.me,
    enabled: hasToken && bootstrapped,
    retry: false,
  });

  useEffect(() => {
    setBootstrapped(true);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: meQuery.data ?? null,
      loading: !bootstrapped || (hasToken && meQuery.isLoading),
      isAuthenticated: Boolean(meQuery.data),
      async login(username, password) {
        const result = await authApi.login(username, password);
        queryClient.setQueryData(["auth", "me"], result.user);
        return result.user;
      },
      async logout() {
        await authApi.logout();
        queryClient.setQueryData(["auth", "me"], null);
        queryClient.clear();
      },
      async refreshMe() {
        await queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      },
    }),
    [bootstrapped, hasToken, meQuery.data, meQuery.isLoading, queryClient],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
