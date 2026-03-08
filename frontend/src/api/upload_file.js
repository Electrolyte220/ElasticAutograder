const API_BASE = "http://localhost:8080/api";

export async function uploadFile(file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE}/jobs/upload`, {
    method: "POST",
    body: formData
  });

  const message = await response.text();

  if (!response.ok) throw new Error(message);
  return message;
}
