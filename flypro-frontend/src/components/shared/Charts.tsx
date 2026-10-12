import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { ChartDto } from "@/types/api";

const COLORS = ["#1f745a", "#c47f17", "#c0392b", "#485c6e", "#47ad8a", "#768ea1"];

function toSeriesData(chart: ChartDto) {
  return chart.labels.map((label, i) => {
    const point: Record<string, string | number> = { label };
    chart.series.forEach((s) => {
      point[s.name] = s.data[i] ?? 0;
    });
    return point;
  });
}

export function AppLineChart({ chart }: { chart: ChartDto }) {
  const data = toSeriesData(chart);
  return (
    <ResponsiveContainer width="100%" height={280}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#d7e0e7" />
        <XAxis dataKey="label" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip />
        <Legend />
        {chart.series.map((s, i) => (
          <Line key={s.name} type="monotone" dataKey={s.name} stroke={COLORS[i % COLORS.length]} strokeWidth={2} />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );
}

export function AppBarChart({ chart }: { chart: ChartDto }) {
  const data = toSeriesData(chart);
  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#d7e0e7" />
        <XAxis dataKey="label" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip />
        <Legend />
        {chart.series.map((s, i) => (
          <Bar key={s.name} dataKey={s.name} fill={COLORS[i % COLORS.length]} radius={[4, 4, 0, 0]} />
        ))}
      </BarChart>
    </ResponsiveContainer>
  );
}

export function AppAreaChart({ chart }: { chart: ChartDto }) {
  const data = toSeriesData(chart);
  return (
    <ResponsiveContainer width="100%" height={280}>
      <AreaChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#d7e0e7" />
        <XAxis dataKey="label" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip />
        <Legend />
        {chart.series.map((s, i) => (
          <Area
            key={s.name}
            type="monotone"
            dataKey={s.name}
            stroke={COLORS[i % COLORS.length]}
            fill={COLORS[i % COLORS.length]}
            fillOpacity={0.2}
          />
        ))}
      </AreaChart>
    </ResponsiveContainer>
  );
}

export function AppPieChart({ chart }: { chart: ChartDto }) {
  const series = chart.series[0];
  const data = chart.labels.map((label, i) => ({ name: label, value: series?.data[i] ?? 0 }));
  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie data={data} dataKey="value" nameKey="name" outerRadius={100} label>
          {data.map((_, i) => (
            <Cell key={i} fill={COLORS[i % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
