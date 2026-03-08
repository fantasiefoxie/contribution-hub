import { useState } from "react";
import { Copy, Key, Webhook, BookOpen, Code2, Shield, Zap, ChevronRight, Plus, Trash2, Eye, EyeOff, Loader2, ExternalLink } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import { useProtocolDocs, useApiKeys, useCreateApiKey, useRevokeApiKey, useWebhooks, useCreateWebhook } from "@/hooks/use-protocol";

const AVAILABLE_SCOPES = ["users", "tasks", "initiatives", "contributions", "events", "webhooks", "*"];
const EVENT_TYPES = ["TaskCreated", "TaskReserved", "TaskClaimed", "TaskSubmitted", "ValidationApproved", "ValidationRejected", "ContributionRecorded", "InitiativeCreated"];

export default function DeveloperPortal() {
  const { data: docsData } = useProtocolDocs();
  const { data: keysData, isLoading: loadingKeys } = useApiKeys();
  const { data: webhooksData, isLoading: loadingWebhooks } = useWebhooks();
  const createKeyMutation = useCreateApiKey();
  const revokeKeyMutation = useRevokeApiKey();
  const createWebhookMutation = useCreateWebhook();

  const [newKeyName, setNewKeyName] = useState("");
  const [selectedScopes, setSelectedScopes] = useState<string[]>(["tasks", "initiatives"]);
  const [revealedKey, setRevealedKey] = useState<string | null>(null);
  const [webhookUrl, setWebhookUrl] = useState("");
  const [webhookKeyId, setWebhookKeyId] = useState("");
  const [webhookEvents, setWebhookEvents] = useState<string[]>(["*"]);

  const docs = docsData;
  const apiKeys = keysData?.api_keys ?? [];
  const webhooks = webhooksData?.webhooks ?? [];

  const handleCreateKey = async () => {
    if (!newKeyName.trim()) return toast.error("Name is required");
    try {
      const result = await createKeyMutation.mutateAsync({ name: newKeyName.trim(), scopes: selectedScopes });
      setRevealedKey(result.api_key.key);
      setNewKeyName("");
      toast.success("API key created! Copy it now — it won't be shown again.");
    } catch (e: any) {
      toast.error(e.message);
    }
  };

  const handleRevokeKey = async (keyId: string) => {
    try {
      await revokeKeyMutation.mutateAsync(keyId);
      toast.success("Key revoked");
    } catch (e: any) {
      toast.error(e.message);
    }
  };

  const handleCreateWebhook = async () => {
    if (!webhookUrl.trim() || !webhookKeyId) return toast.error("URL and API key are required");
    try {
      const result = await createWebhookMutation.mutateAsync({ api_key_id: webhookKeyId, url: webhookUrl.trim(), events: webhookEvents });
      toast.success(`Webhook created. Secret: ${result.secret.substring(0, 12)}...`);
      setWebhookUrl("");
    } catch (e: any) {
      toast.error(e.message);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success("Copied to clipboard");
  };

  const baseUrl = import.meta.env.VITE_SUPABASE_URL + "/functions/v1/protocol";

  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center gap-2">
          <Code2 className="h-5 w-5 text-primary" />
          <h1 className="font-display text-2xl font-bold">Developer Portal</h1>
        </div>
        <p className="text-sm text-muted-foreground mt-1">Build on the Contribution OS Coordination Protocol</p>
      </div>

      <Tabs defaultValue="docs" className="px-4">
        <TabsList className="grid w-full grid-cols-4 mb-4 rounded-xl bg-muted/60">
          <TabsTrigger value="docs" className="rounded-lg text-xs data-[state=active]:bg-card">Docs</TabsTrigger>
          <TabsTrigger value="keys" className="rounded-lg text-xs data-[state=active]:bg-card">API Keys</TabsTrigger>
          <TabsTrigger value="webhooks" className="rounded-lg text-xs data-[state=active]:bg-card">Webhooks</TabsTrigger>
          <TabsTrigger value="events" className="rounded-lg text-xs data-[state=active]:bg-card">Events</TabsTrigger>
        </TabsList>

        {/* ── Docs Tab ── */}
        <TabsContent value="docs" className="space-y-4">
          {/* Base URL */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-2">Base URL</h3>
            <div className="flex items-center gap-2 bg-muted rounded-lg p-2">
              <code className="text-xs font-mono flex-1 break-all text-foreground">{baseUrl}</code>
              <Button variant="ghost" size="sm" onClick={() => copyToClipboard(baseUrl)}>
                <Copy className="h-3 w-3" />
              </Button>
            </div>
          </div>

          {/* Auth */}
          <div className="bg-card rounded-xl border border-border p-4">
            <div className="flex items-center gap-2 mb-3">
              <Shield className="h-4 w-4 text-primary" />
              <h3 className="font-display font-semibold text-sm">Authentication</h3>
            </div>
            <div className="space-y-2 text-xs text-muted-foreground">
              <div className="bg-muted rounded-lg p-3">
                <p className="font-medium text-foreground mb-1">API Key</p>
                <code className="text-[10px] font-mono">x-api-key: cos_&lt;64 hex chars&gt;</code>
              </div>
              <div className="bg-muted rounded-lg p-3">
                <p className="font-medium text-foreground mb-1">Bearer Token</p>
                <code className="text-[10px] font-mono">Authorization: Bearer &lt;jwt&gt;</code>
              </div>
            </div>
          </div>

          {/* Idempotency */}
          <div className="bg-card rounded-xl border border-border p-4">
            <div className="flex items-center gap-2 mb-2">
              <Shield className="h-4 w-4 text-accent" />
              <h3 className="font-display font-semibold text-sm">Idempotency</h3>
            </div>
            <p className="text-xs text-muted-foreground mb-2">
              Pass <code className="font-mono text-foreground">x-idempotency-key</code> header on POST requests to ensure at-most-once delivery. Keys expire after 24h.
            </p>
            <div className="bg-muted rounded-lg p-2">
              <code className="text-[10px] font-mono">x-idempotency-key: unique-request-id-123</code>
            </div>
          </div>

          {/* Rate Limits */}
          <div className="bg-card rounded-xl border border-border p-4">
            <div className="flex items-center gap-2 mb-2">
              <Zap className="h-4 w-4 text-secondary" />
              <h3 className="font-display font-semibold text-sm">Rate Limits</h3>
            </div>
            <div className="space-y-1 text-xs text-muted-foreground">
              <p>API Key: <span className="font-medium text-foreground">60 req/min</span> (configurable per key)</p>
              <p>Bearer Token: <span className="font-medium text-foreground">120 req/min</span></p>
              <p className="text-[10px] mt-1">Returns <code className="font-mono">429 Too Many Requests</code> when exceeded.</p>
            </div>
          </div>

          {/* Scopes */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-2">Permission Scopes</h3>
            <p className="text-[10px] text-muted-foreground mb-2">Legacy scopes (backward compatible):</p>
            <div className="flex flex-wrap gap-1.5 mb-3">
              {AVAILABLE_SCOPES.map(scope => (
                <Badge key={scope} variant="outline" className="text-[10px] font-mono">{scope}</Badge>
              ))}
            </div>
            <p className="text-[10px] text-muted-foreground mb-2">Granular scopes (v2):</p>
            <div className="flex flex-wrap gap-1.5">
              {["tasks:read", "tasks:create", "tasks:claim", "tasks:validate", "tasks:manage",
                "initiatives:read", "initiatives:create", "initiatives:manage",
                "contributions:read", "profiles:read", "submissions:review"].map(scope => (
                <Badge key={scope} variant="outline" className="text-[10px] font-mono">{scope}</Badge>
              ))}
            </div>
          </div>

          {/* Event Versioning */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-2">Event Versioning</h3>
            <p className="text-xs text-muted-foreground">
              All events include a <code className="font-mono text-foreground">version</code> field. Current version: <span className="font-medium text-foreground">1</span>.
              Filter with <code className="font-mono text-foreground">?version=1</code>.
            </p>
          </div>

          {/* Endpoints */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3">Endpoints</h3>
            <div className="space-y-2">
              {(docs?.endpoints ?? []).map((ep: any, i: number) => (
                <div key={i} className="flex items-start gap-2 bg-muted rounded-lg p-2.5">
                  <Badge className={`text-[9px] font-mono shrink-0 ${
                    ep.method === "GET" ? "bg-accent/10 text-accent border-accent/20" :
                    ep.method === "POST" ? "bg-primary/10 text-primary border-primary/20" :
                    "bg-destructive/10 text-destructive border-destructive/20"
                  }`}>{ep.method}</Badge>
                  <div className="min-w-0 flex-1">
                    <code className="text-[11px] font-mono text-foreground">{ep.path}</code>
                    <p className="text-[10px] text-muted-foreground mt-0.5">{ep.description}</p>
                    {ep.scope && (
                      <Badge variant="outline" className="text-[8px] font-mono mt-1">scope: {ep.scope}</Badge>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
          </div>
        </TabsContent>

        {/* ── API Keys Tab ── */}
        <TabsContent value="keys" className="space-y-4">
          {/* Create Key */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Key className="h-4 w-4 text-primary" />
              Create API Key
            </h3>
            <Input
              placeholder="Key name (e.g., My App)"
              value={newKeyName}
              onChange={e => setNewKeyName(e.target.value)}
              className="mb-3 rounded-lg"
            />
            <div className="mb-3">
              <p className="text-xs text-muted-foreground mb-1.5">Scopes</p>
              <div className="flex flex-wrap gap-1.5">
                {AVAILABLE_SCOPES.map(scope => (
                  <button
                    key={scope}
                    onClick={() => setSelectedScopes(prev =>
                      prev.includes(scope) ? prev.filter(s => s !== scope) : [...prev, scope]
                    )}
                    className={`text-[10px] font-mono px-2 py-1 rounded-md border transition-colors ${
                      selectedScopes.includes(scope)
                        ? "bg-primary/10 text-primary border-primary/30"
                        : "bg-muted text-muted-foreground border-border hover:border-primary/20"
                    }`}
                  >{scope}</button>
                ))}
              </div>
            </div>
            <Button onClick={handleCreateKey} disabled={createKeyMutation.isPending} size="sm" className="rounded-lg gap-1.5">
              {createKeyMutation.isPending ? <Loader2 className="h-3 w-3 animate-spin" /> : <Plus className="h-3 w-3" />}
              Generate Key
            </Button>
          </div>

          {/* Revealed Key */}
          {revealedKey && (
            <div className="bg-primary/5 rounded-xl border border-primary/20 p-4">
              <p className="text-xs font-semibold text-primary mb-2">⚠️ Copy your API key now — it won't be shown again</p>
              <div className="flex items-center gap-2 bg-card rounded-lg p-2 border border-border">
                <code className="text-[10px] font-mono flex-1 break-all">{revealedKey}</code>
                <Button variant="ghost" size="sm" onClick={() => copyToClipboard(revealedKey)}>
                  <Copy className="h-3 w-3" />
                </Button>
              </div>
              <Button variant="ghost" size="sm" className="mt-2 text-xs" onClick={() => setRevealedKey(null)}>
                Dismiss
              </Button>
            </div>
          )}

          {/* Keys List */}
          <div className="space-y-2">
            {loadingKeys && <p className="text-xs text-muted-foreground text-center py-4">Loading keys...</p>}
            {apiKeys.map((key: any) => (
              <div key={key.id} className="bg-card rounded-xl border border-border p-3 flex items-start gap-3">
                <Key className={`h-4 w-4 mt-0.5 shrink-0 ${key.is_active ? "text-primary" : "text-muted-foreground"}`} />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium">{key.name}</p>
                    {!key.is_active && <Badge variant="outline" className="text-[8px] text-destructive border-destructive/20">Revoked</Badge>}
                  </div>
                  <code className="text-[10px] font-mono text-muted-foreground">{key.key_prefix}••••••••</code>
                  <div className="flex flex-wrap gap-1 mt-1.5">
                    {(key.scopes || []).map((s: string) => (
                      <Badge key={s} variant="outline" className="text-[8px] font-mono">{s}</Badge>
                    ))}
                  </div>
                </div>
                {key.is_active && (
                  <Button variant="ghost" size="sm" onClick={() => handleRevokeKey(key.id)} className="text-destructive hover:text-destructive">
                    <Trash2 className="h-3 w-3" />
                  </Button>
                )}
              </div>
            ))}
            {!loadingKeys && apiKeys.length === 0 && (
              <p className="text-xs text-muted-foreground text-center py-6">No API keys yet. Create one to get started.</p>
            )}
          </div>
        </TabsContent>

        {/* ── Webhooks Tab ── */}
        <TabsContent value="webhooks" className="space-y-4">
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Webhook className="h-4 w-4 text-accent" />
              Create Webhook
            </h3>
            <Input
              placeholder="Webhook URL (https://...)"
              value={webhookUrl}
              onChange={e => setWebhookUrl(e.target.value)}
              className="mb-3 rounded-lg"
            />
            {apiKeys.length > 0 && (
              <select
                value={webhookKeyId}
                onChange={e => setWebhookKeyId(e.target.value)}
                className="w-full text-xs border border-border rounded-lg p-2 mb-3 bg-background"
              >
                <option value="">Select API key...</option>
                {apiKeys.filter((k: any) => k.is_active).map((k: any) => (
                  <option key={k.id} value={k.id}>{k.name}</option>
                ))}
              </select>
            )}
            <div className="mb-3">
              <p className="text-xs text-muted-foreground mb-1.5">Events</p>
              <div className="flex flex-wrap gap-1.5">
                {["*", ...EVENT_TYPES].map(evt => (
                  <button
                    key={evt}
                    onClick={() => setWebhookEvents(prev =>
                      prev.includes(evt) ? prev.filter(e => e !== evt) : [...prev, evt]
                    )}
                    className={`text-[10px] font-mono px-2 py-1 rounded-md border transition-colors ${
                      webhookEvents.includes(evt)
                        ? "bg-accent/10 text-accent border-accent/30"
                        : "bg-muted text-muted-foreground border-border hover:border-accent/20"
                    }`}
                  >{evt}</button>
                ))}
              </div>
            </div>
            <Button onClick={handleCreateWebhook} disabled={createWebhookMutation.isPending} size="sm" className="rounded-lg gap-1.5">
              {createWebhookMutation.isPending ? <Loader2 className="h-3 w-3 animate-spin" /> : <Plus className="h-3 w-3" />}
              Create Webhook
            </Button>
          </div>

          <div className="space-y-2">
            {loadingWebhooks && <p className="text-xs text-muted-foreground text-center py-4">Loading webhooks...</p>}
            {webhooks.map((wh: any) => (
              <div key={wh.id} className="bg-card rounded-xl border border-border p-3">
                <div className="flex items-center gap-2 mb-1">
                  <Webhook className="h-3 w-3 text-accent" />
                  <code className="text-[10px] font-mono text-foreground truncate">{wh.url}</code>
                  {!wh.is_active && <Badge variant="outline" className="text-[8px] text-destructive">Disabled</Badge>}
                </div>
                <div className="flex flex-wrap gap-1 mt-1">
                  {(wh.events || []).map((e: string) => (
                    <Badge key={e} variant="outline" className="text-[8px] font-mono">{e}</Badge>
                  ))}
                </div>
                {wh.failure_count > 0 && (
                  <p className="text-[10px] text-destructive mt-1">⚠ {wh.failure_count} delivery failures</p>
                )}
              </div>
            ))}
            {!loadingWebhooks && webhooks.length === 0 && (
              <p className="text-xs text-muted-foreground text-center py-6">No webhooks configured yet.</p>
            )}
          </div>
        </TabsContent>

        {/* ── Events Tab ── */}
        <TabsContent value="events" className="space-y-4">
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3">Event Types</h3>
            <p className="text-xs text-muted-foreground mb-3">
              Subscribe to these events via webhooks or poll the <code className="font-mono">/events</code> endpoint.
            </p>
            <div className="space-y-2">
              {EVENT_TYPES.map(evt => (
                <div key={evt} className="bg-muted rounded-lg p-2.5 flex items-center gap-2">
                  <Zap className="h-3 w-3 text-secondary shrink-0" />
                  <code className="text-[11px] font-mono text-foreground">{evt}</code>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-2">Webhook Payload Format</h3>
            <pre className="bg-muted rounded-lg p-3 text-[10px] font-mono text-foreground overflow-x-auto">
{`{
  "event": "TaskCreated",
  "data": { ... },
  "timestamp": "2026-03-08T12:00:00Z"
}

Headers:
  X-Webhook-Signature: HMAC-SHA256
  X-Event-Type: TaskCreated`}
            </pre>
          </div>

          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-2">Quick Start</h3>
            <pre className="bg-muted rounded-lg p-3 text-[10px] font-mono text-foreground overflow-x-auto whitespace-pre-wrap">
{`# List public tasks
curl -H "x-api-key: cos_YOUR_KEY" \\
  ${baseUrl}/tasks

# Create a task
curl -X POST \\
  -H "x-api-key: cos_YOUR_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"title":"Clean up park"}' \\
  ${baseUrl}/tasks

# Query events since a date
curl -H "x-api-key: cos_YOUR_KEY" \\
  "${baseUrl}/events?since=2026-03-01"`}
            </pre>
          </div>
        </TabsContent>
      </Tabs>

      <div className="h-6" />
    </AppShell>
  );
}
