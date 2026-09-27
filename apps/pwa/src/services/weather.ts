import { apiFetch } from "./api";

export type WeatherData = {
  location: { latitude: number; longitude: number; timezone?: string; elevation_m?: number };
  current: {
    temperature_2m?: number;
    relative_humidity_2m?: number;
    precipitation?: number;
    rain?: number;
    wind_speed_10m?: number;
    weather_code?: number;
    weather_description?: string;
    time?: string;
  };
  forecast: Array<{
    date: string;
    temperature_max_c?: number;
    temperature_min_c?: number;
    precipitation_probability_max_pct?: number;
    precipitation_sum_mm?: number;
    wind_speed_max_kmh?: number;
    et0_mm?: number;
  }>;
  farm_alert: string;
  farm_alerts: string[];
  language: string;
  source: string;
  safety: { status: string; reason: string };
};

export async function getWeather(latitude: number, longitude: number, language: string, forecastDays = 5): Promise<WeatherData> {
  const params = new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    language,
    forecast_days: String(forecastDays),
  });
  const response = await apiFetch(`/api/v1/weather?${params.toString()}`);
  if (!response.ok) {
    let message = "Weather information is temporarily unavailable.";
    try {
      const payload = await response.json();
      if (typeof payload?.detail === "string") message = payload.detail;
      else if (typeof payload?.detail?.message === "string") message = payload.detail.message;
    } catch {}
    throw new Error(message);
  }
  return response.json() as Promise<WeatherData>;
}
