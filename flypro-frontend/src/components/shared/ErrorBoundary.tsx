import { Component, type ErrorInfo, type ReactNode } from "react";
import { Button } from "@/components/ui/Button";

type Props = { children: ReactNode };
type State = { hasError: boolean };

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("UI error", error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
          <h1 className="ram-wordmark text-3xl text-brand-600">Une erreur est survenue</h1>
          <p className="text-sm text-[var(--color-ink-500)]">Rechargez la page ou revenez au dashboard.</p>
          <Button onClick={() => window.location.assign("/dashboard")}>Retour dashboard</Button>
        </div>
      );
    }
    return this.props.children;
  }
}
