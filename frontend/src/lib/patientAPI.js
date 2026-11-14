
export async function apiRequest(url, options = {}) {
  const {
    method = "GET",
    headers = {},
    body,
    signal,
    token
  } = options;

  const requestHeaders = { ...headers };

  if (token) {
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
    let errorMessage = `Request failed with status: ${response.status}`;

    try {
      const responseText = await response.text();

      if (responseText) {
        try {
          const errorData = JSON.parse(responseText);

          errorMessage =
            errorData.message ||
            errorData.error ||
            errorData.detail ||
            responseText;
        } catch {
          errorMessage = responseText;
        }
      }
    } catch {
      // Keep the status-based fallback message.
    }

    throw new Error(errorMessage);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}
