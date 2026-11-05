export function getPatients(url, signal) {
  return fetch(url, { signal })
    .then(response => {
      if (!response.ok) {
        throw new Error("Failed to fetch patients");
      }

      return response.json();
    });
}