import { Moon, Sun } from "lucide-react";
import { useTheme } from "./ThemeProvider";

export default function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const isDark = theme === "dark" || (theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);

  return (
    <button
      onClick={() => setTheme(isDark ? "light" : "dark")}
      className="h-10 w-10 rounded-full bg-muted flex items-center justify-center hover:bg-muted/80 transition-colors"
      aria-label="Toggle theme"
    >
      {isDark ? <Sun className="h-4.5 w-4.5 text-secondary" /> : <Moon className="h-4.5 w-4.5 text-muted-foreground" />}
    </button>
  );
}
