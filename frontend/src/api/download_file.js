const API_BASE = "https://elastic-autograder-backend.onrender.com/api";

export async function downloadResults(jobId) {
  const response = await fetch(`${API_BASE}/jobs/result/${jobId}`);

  const message = await response.blob();

  if (!response.ok) throw new Error(message.message);
  return message;
}
