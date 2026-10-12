import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapContainer, Marker, Popup, Rectangle, Tooltip, useMap } from "react-leaflet";
import { Activity, Building2, DoorOpen, MapPin, ShieldAlert } from "lucide-react";
import { accessEventsApi, accessPointsApi, zonesApi } from "@/api/services";
import { PageHeader } from "@/components/shared/PageHeader";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { Checkbox, Label, Select } from "@/components/ui/FormControls";
import { PageLoader } from "@/components/ui/Feedback";
import type { AccessPoint, AccessPointStatus, AccessResult, Zone } from "@/types/api";
import { formatDate } from "@/utils/cn";

const ZONE_W = 200;
const ZONE_H = 130;
const GAP_X = 36;
const GAP_Y = 36;
const COLS = 3;
const BUILDING_GAP = 90;
const BUILDING_PAD = 26;

const STATUS_COLOR: Record<AccessPointStatus, string> = {
  ACTIVE: "#16a34a",
  INACTIVE: "#94a3b8",
  MAINTENANCE: "#f59e0b",
  OUT_OF_SERVICE: "#dc2626",
};

const STATUS_LABEL: Record<AccessPointStatus, string> = {
  ACTIVE: "Actif",
  INACTIVE: "Inactif",
  MAINTENANCE: "Maintenance",
  OUT_OF_SERVICE: "Hors service",
};

type ZoneLayout = { zone: Zone; x: number; yTop: number; yBottom: number };
type BuildingLayout = { name: string; xMin: number; xMax: number; yMin: number; yMax: number };
type PlacedPoint = { point: AccessPoint; x: number; y: number };

function layoutZones(zones: Zone[]): { zoneLayouts: ZoneLayout[]; buildingLayouts: BuildingLayout[] } {
  const byBuilding = new Map<string, Zone[]>();
  zones.forEach((z) => {
    const list = byBuilding.get(z.building) ?? [];
    list.push(z);
    byBuilding.set(z.building, list);
  });

  const buildingNames = Array.from(byBuilding.keys()).sort();
  const zoneLayouts: ZoneLayout[] = [];
  const buildingLayouts: BuildingLayout[] = [];
  let cursorX = 0;

  buildingNames.forEach((name) => {
    const list = byBuilding.get(name) ?? [];
    const cols = Math.min(COLS, Math.max(1, list.length));
    const rows = Math.ceil(list.length / cols);
    const blockW = cols * ZONE_W + (cols - 1) * GAP_X;
    const blockH = rows * ZONE_H + (rows - 1) * GAP_Y;

    list.forEach((zone, idx) => {
      const col = idx % cols;
      const row = Math.floor(idx / cols);
      const x = cursorX + col * (ZONE_W + GAP_X);
      const yTop = -row * (ZONE_H + GAP_Y);
      const yBottom = yTop - ZONE_H;
      zoneLayouts.push({ zone, x, yTop, yBottom });
    });

    buildingLayouts.push({
      name,
      xMin: cursorX - BUILDING_PAD,
      xMax: cursorX + blockW + BUILDING_PAD,
      yMin: -blockH - BUILDING_PAD,
      yMax: BUILDING_PAD,
    });

    cursorX += blockW + BUILDING_GAP;
  });

  return { zoneLayouts, buildingLayouts };
}

function layoutAccessPoints(points: AccessPoint[], zoneLayouts: ZoneLayout[]): PlacedPoint[] {
  const zoneMap = new Map(zoneLayouts.map((zl) => [zl.zone.id, zl]));
  const byZone = new Map<number, AccessPoint[]>();
  points.forEach((p) => {
    const list = byZone.get(p.zone.id) ?? [];
    list.push(p);
    byZone.set(p.zone.id, list);
  });

  const placed: PlacedPoint[] = [];
  byZone.forEach((list, zoneId) => {
    const zl = zoneMap.get(zoneId);
    if (!zl) return;
    const n = list.length;
    list.forEach((point, idx) => {
      const x = zl.x + ((idx + 1) / (n + 1)) * ZONE_W;
      placed.push({ point, x, y: zl.yBottom });
    });
  });
  return placed;
}

function pointDivIcon(status: AccessPointStatus) {
  const color = STATUS_COLOR[status];
  return L.divIcon({
    className: "",
    html: `<div style="width:14px;height:14px;border-radius:9999px;background:${color};border:2px solid white;box-shadow:0 1px 3px rgba(0,0,0,0.45)"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
  });
}

function pulseDivIcon(result: AccessResult) {
  const color = result === "AUTHORIZED" ? "#16a34a" : "#dc2626";
  return L.divIcon({
    className: "",
    html: `<div style="position:relative;width:24px;height:24px;">
      <span class="animate-ping" style="position:absolute;inset:0;border-radius:9999px;background:${color};opacity:0.55;"></span>
      <span style="position:absolute;inset:7px;border-radius:9999px;background:${color};border:2px solid white;"></span>
    </div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });
}

function buildingLabelIcon(name: string) {
  return L.divIcon({
    className: "",
    html: `<div style="transform:translate(4px,-4px);font-size:11px;font-weight:700;letter-spacing:0.04em;text-transform:uppercase;color:#7a1220;background:rgba(255,255,255,0.92);padding:3px 8px;border-radius:8px;border:1px solid #e6c8cc;white-space:nowrap;box-shadow:0 1px 3px rgba(0,0,0,0.15);">${name}</div>`,
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  });
}

function FitToBounds({ bounds }: { bounds: L.LatLngBoundsExpression | null }) {
  const map = useMap();
  useEffect(() => {
    if (bounds) map.fitBounds(bounds, { padding: [40, 40] });
  }, [bounds, map]);
  return null;
}

export function DigitalTwinPage() {
  const [building, setBuilding] = useState("");
  const [sensitiveOnly, setSensitiveOnly] = useState(false);

  const zonesQuery = useQuery({
    queryKey: ["zones", "twin"],
    queryFn: () => zonesApi.list({ page: 0, size: 200, sort: "building,asc" }),
  });
  const pointsQuery = useQuery({
    queryKey: ["access-points", "twin"],
    queryFn: () => accessPointsApi.list({ page: 0, size: 500 }),
  });
  const eventsQuery = useQuery({
    queryKey: ["access-events", "twin"],
    queryFn: () => accessEventsApi.list({ page: 0, size: 15, sort: "eventTimestamp,desc" }),
    refetchInterval: 10_000,
  });

  const allZones = zonesQuery.data?.content ?? [];
  const buildings = useMemo(
    () => Array.from(new Set(allZones.map((z) => z.building))).sort(),
    [allZones],
  );

  const filteredZones = useMemo(
    () =>
      allZones.filter(
        (z) => (!building || z.building === building) && (!sensitiveOnly || z.sensitive),
      ),
    [allZones, building, sensitiveOnly],
  );

  const { zoneLayouts, buildingLayouts } = useMemo(() => layoutZones(filteredZones), [filteredZones]);

  const zoneIds = useMemo(() => new Set(filteredZones.map((z) => z.id)), [filteredZones]);
  const allPoints = pointsQuery.data?.content ?? [];
  const filteredPoints = useMemo(
    () => allPoints.filter((p) => zoneIds.has(p.zone.id)),
    [allPoints, zoneIds],
  );
  const placedPoints = useMemo(
    () => layoutAccessPoints(filteredPoints, zoneLayouts),
    [filteredPoints, zoneLayouts],
  );

  const pointById = useMemo(() => new Map(placedPoints.map((pp) => [pp.point.id, pp])), [placedPoints]);
  const zoneById = useMemo(() => new Map(zoneLayouts.map((zl) => [zl.zone.id, zl])), [zoneLayouts]);

  const events = eventsQuery.data?.content ?? [];
  const pulses = useMemo(() => {
    return events
      .map((event) => {
        const pp = pointById.get(event.accessPointId);
        if (pp) return { event, position: [pp.y, pp.x] as [number, number] };
        const zoneId = event.zone?.id;
        if (zoneId != null) {
          const zl = zoneById.get(zoneId);
          if (zl) return { event, position: [(zl.yTop + zl.yBottom) / 2, zl.x + ZONE_W / 2] as [number, number] };
        }
        return null;
      })
      .filter((v): v is { event: (typeof events)[number]; position: [number, number] } => v !== null);
  }, [events, pointById, zoneById]);

  const overallBounds = useMemo<L.LatLngBoundsExpression | null>(() => {
    if (buildingLayouts.length === 0) return null;
    const xMin = Math.min(...buildingLayouts.map((b) => b.xMin));
    const xMax = Math.max(...buildingLayouts.map((b) => b.xMax));
    const yMin = Math.min(...buildingLayouts.map((b) => b.yMin));
    const yMax = Math.max(...buildingLayouts.map((b) => b.yMax));
    return [
      [yMin, xMin],
      [yMax, xMax],
    ];
  }, [buildingLayouts]);

  const statusCounts = useMemo(() => {
    const counts: Record<AccessPointStatus, number> = { ACTIVE: 0, INACTIVE: 0, MAINTENANCE: 0, OUT_OF_SERVICE: 0 };
    filteredPoints.forEach((p) => {
      counts[p.status] += 1;
    });
    return counts;
  }, [filteredPoints]);

  if (zonesQuery.isLoading || pointsQuery.isLoading) return <PageLoader />;

  return (
    <div>
      <PageHeader
        title="Digital Twin"
        description="Plan schématique 2D — zones, points d'accès et événements récents en direct"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Digital Twin" }]}
      />

      <div className="mb-4 grid gap-3 sm:grid-cols-[220px_auto_1fr] sm:items-end">
        <div>
          <Label>Bâtiment</Label>
          <Select value={building} onChange={(e) => setBuilding(e.target.value)}>
            <option value="">Tous</option>
            {buildings.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </Select>
        </div>
        <div className="pb-2">
          <Checkbox
            label="Zones sensibles uniquement"
            checked={sensitiveOnly}
            onChange={(e) => setSensitiveOnly(e.target.checked)}
          />
        </div>
        <div className="flex items-center justify-end gap-2 text-xs text-[var(--color-ink-500)]">
          <Activity className="size-3.5 animate-pulse text-brand-600" />
          Rafraîchissement automatique — 10s
        </div>
      </div>

      <div className="mb-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <Card>
          <CardBody className="flex items-center gap-3">
            <MapPin className="size-5 text-brand-600" />
            <div>
              <p className="text-xl font-bold tabular-nums">{filteredZones.length}</p>
              <p className="text-xs text-[var(--color-ink-500)]">Zones affichées</p>
            </div>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="flex items-center gap-3">
            <ShieldAlert className="size-5 text-red-600" />
            <div>
              <p className="text-xl font-bold tabular-nums">{filteredZones.filter((z) => z.sensitive).length}</p>
              <p className="text-xs text-[var(--color-ink-500)]">Zones sensibles</p>
            </div>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="flex items-center gap-3">
            <DoorOpen className="size-5" style={{ color: STATUS_COLOR.ACTIVE }} />
            <div>
              <p className="text-xl font-bold tabular-nums">{statusCounts.ACTIVE}</p>
              <p className="text-xs text-[var(--color-ink-500)]">Points actifs</p>
            </div>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="flex items-center gap-3">
            <DoorOpen className="size-5" style={{ color: STATUS_COLOR.MAINTENANCE }} />
            <div>
              <p className="text-xl font-bold tabular-nums">{statusCounts.MAINTENANCE}</p>
              <p className="text-xs text-[var(--color-ink-500)]">En maintenance</p>
            </div>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="flex items-center gap-3">
            <DoorOpen className="size-5" style={{ color: STATUS_COLOR.OUT_OF_SERVICE }} />
            <div>
              <p className="text-xl font-bold tabular-nums">{statusCounts.OUT_OF_SERVICE}</p>
              <p className="text-xs text-[var(--color-ink-500)]">Hors service</p>
            </div>
          </CardBody>
        </Card>
      </div>

      <Card className="overflow-hidden">
        <CardHeader
          title="Plan du siège"
          description="Cliquez sur une zone ou un point d'accès pour le détail — pastilles animées = accès des dernières minutes"
          action={<Building2 className="size-4 text-brand-600" />}
        />
        <CardBody className="p-0">
          {filteredZones.length === 0 ? (
            <div className="p-12 text-center text-sm text-[var(--color-ink-500)]">
              Aucune zone à afficher avec ces filtres.
            </div>
          ) : (
            <div className="h-[560px] w-full bg-[var(--color-surface-muted)]">
              <MapContainer
                crs={L.CRS.Simple}
                center={[0, 0]}
                zoom={0}
                minZoom={-4}
                maxZoom={3}
                style={{ height: "100%", width: "100%" }}
                attributionControl={false}
              >
                <FitToBounds bounds={overallBounds} />

                {buildingLayouts.map((b) => (
                  <Rectangle
                    key={b.name}
                    bounds={[
                      [b.yMin, b.xMin],
                      [b.yMax, b.xMax],
                    ]}
                    pathOptions={{ color: "#94a3b8", weight: 1, dashArray: "6 6", fillOpacity: 0.03 }}
                  />
                ))}
                {buildingLayouts.map((b) => (
                  <Marker key={`${b.name}-label`} position={[b.yMax, b.xMin]} icon={buildingLabelIcon(b.name)} interactive={false} />
                ))}

                {zoneLayouts.map((zl) => (
                  <Rectangle
                    key={zl.zone.id}
                    bounds={[
                      [zl.yBottom, zl.x],
                      [zl.yTop, zl.x + ZONE_W],
                    ]}
                    pathOptions={{
                      color: zl.zone.sensitive ? "#dc2626" : "#C2002F",
                      weight: zl.zone.sensitive ? 2.5 : 1.5,
                      fillColor: zl.zone.sensitive ? "#fecaca" : "#fde4e9",
                      fillOpacity: 0.5,
                    }}
                  >
                    <Tooltip direction="center" permanent opacity={1}>
                      <div className="text-center text-[11px] leading-tight font-semibold text-[#5b1420]">
                        {zl.zone.name}
                        <div className="font-normal text-[#8a5a60]">{zl.zone.floor}</div>
                      </div>
                    </Tooltip>
                    <Popup>
                      <div className="text-sm">
                        <p className="font-semibold">{zl.zone.name}</p>
                        <p className="text-xs text-[var(--color-ink-500)]">
                          {zl.zone.building} · {zl.zone.floor}
                        </p>
                        {zl.zone.sensitive ? (
                          <p className="mt-1 text-xs font-semibold text-red-600">Zone sensible</p>
                        ) : null}
                        {zl.zone.description ? <p className="mt-1 text-xs">{zl.zone.description}</p> : null}
                      </div>
                    </Popup>
                  </Rectangle>
                ))}

                {placedPoints.map((pp) => (
                  <Marker key={pp.point.id} position={[pp.y, pp.x]} icon={pointDivIcon(pp.point.status)}>
                    <Popup>
                      <div className="text-sm">
                        <p className="font-semibold">{pp.point.name}</p>
                        <p className="text-xs text-[var(--color-ink-500)]">{pp.point.code} · {pp.point.type}</p>
                        <p className="mt-1">
                          <StatusBadge
                            tone={
                              pp.point.status === "ACTIVE"
                                ? "success"
                                : pp.point.status === "OUT_OF_SERVICE"
                                  ? "danger"
                                  : pp.point.status === "MAINTENANCE"
                                    ? "warning"
                                    : "neutral"
                            }
                          >
                            {STATUS_LABEL[pp.point.status]}
                          </StatusBadge>
                        </p>
                      </div>
                    </Popup>
                  </Marker>
                ))}

                {pulses.map(({ event, position }) => (
                  <Marker key={event.id} position={position} icon={pulseDivIcon(event.result)} zIndexOffset={1000}>
                    <Popup>
                      <div className="text-sm">
                        <p className="font-semibold">{event.employeeFullName || event.badgeNumber || "Événement"}</p>
                        <p className="text-xs text-[var(--color-ink-500)]">
                          {formatDate(event.eventTimestamp)} · {event.accessPointName}
                        </p>
                        <p className="mt-1">
                          <StatusBadge tone={event.result === "AUTHORIZED" ? "success" : "danger"}>
                            {event.result}
                          </StatusBadge>
                        </p>
                      </div>
                    </Popup>
                  </Marker>
                ))}
              </MapContainer>
            </div>
          )}
        </CardBody>
      </Card>

      <div className="mt-3 flex flex-wrap gap-4 text-xs text-[var(--color-ink-500)]">
        <span className="inline-flex items-center gap-1.5">
          <span className="size-2.5 rounded-full" style={{ background: STATUS_COLOR.ACTIVE }} /> Point actif
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="size-2.5 rounded-full" style={{ background: STATUS_COLOR.MAINTENANCE }} /> Maintenance
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="size-2.5 rounded-full" style={{ background: STATUS_COLOR.OUT_OF_SERVICE }} /> Hors service
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="size-2.5 rounded-full" style={{ background: STATUS_COLOR.INACTIVE }} /> Inactif
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="size-2.5 animate-ping rounded-full" style={{ background: "#16a34a" }} /> Accès autorisé récent
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="size-2.5 animate-ping rounded-full" style={{ background: "#dc2626" }} /> Accès refusé récent
        </span>
      </div>
    </div>
  );
}
