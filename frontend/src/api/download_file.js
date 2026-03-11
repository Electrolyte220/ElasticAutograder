const API_BASE = "http://localhost:8080/api";

export async function downloadResults(jobId) {
  const response = await fetch(`${API_BASE}/jobs/result/${jobId}`);

  const message = await response.blob();

  if (!response.ok) throw new Error(message.message);
  return message;
}
