import { useState } from "react";

interface Props extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallback?: string;
}

export function ImageWithFallback({ src, fallback, alt, ...rest }: Props) {
  const [error, setError] = useState(false);

  return (
    <img
      src={error ? (fallback || "") : (src || "")}
      alt={alt || ""}
      onError={() => setError(true)}
      {...rest}
    />
  );
}
