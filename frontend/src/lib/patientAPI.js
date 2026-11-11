export async function apiRequest(url, options = {}) {
  const {
    method = "GET",
    headers = {},
    body,
    signal,
    token
  } = options;

  const requestHeaders = { ...headers };

  if(token){
    requestHeaders["Authorization"] = `Bearer ${token}`;
  }
    
  if (body !== undefined) {
    requestHeaders["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    method,
    headers: requestHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal
  });

  if (!response.ok) {
    throw new Error(`Request failed with status: ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}