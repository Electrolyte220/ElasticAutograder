const API_BASE = "http://localhost:8080/api";

export async function fetchFileNameFromId(id) {
  const formData = new FormData();
  formData.append("id", id);
  const res = await fetch(`${API_BASE}/jobs/from-id`, {
    method: "POST",
    body: formData
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch file name: ${res.status}`);
  }

  return res.text();
}
