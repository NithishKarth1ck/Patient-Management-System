import "./globals.css";

export const metadata = {
  title: "Patient Management Dashboard",
  description: "A dashboard for managing patient information",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
