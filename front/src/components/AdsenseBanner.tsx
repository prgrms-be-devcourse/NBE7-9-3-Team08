"use client";

import { useEffect, useMemo, useState } from "react";
import type { CSSProperties } from "react";

declare global {
  interface Window {
    adsbygoogle?: Array<Record<string, unknown>>;
  }
}

interface AdsenseBannerProps {
  adSlot: string;
  style?: CSSProperties;
}

export default function AdsenseBanner({ adSlot, style }: AdsenseBannerProps) {
  const clientId = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID;
  const mergedStyle: CSSProperties = style ?? { display: "block" };
  const [forcePreview, setForcePreview] = useState(
    () => process.env.NODE_ENV !== "production"
  );

  useEffect(() => {
    if (process.env.NODE_ENV === "production") {
      const hostname =
        typeof window !== "undefined" ? window.location.hostname : "";
      if (hostname === "localhost" || hostname === "127.0.0.1") {
        setForcePreview(true);
      }
    }
  }, []);

  const showPlaceholder = useMemo(() => {
    return forcePreview || !clientId;
  }, [forcePreview, clientId]);

  useEffect(() => {
    if (showPlaceholder) return;

    try {
      if (typeof window !== "undefined") {
        (window.adsbygoogle = window.adsbygoogle || []).push({});
      }
    } catch (e) {
      console.error("Adsense error", e);
    }
  }, [showPlaceholder]);

  if (showPlaceholder) {
    return (
      <div
        aria-label="광고 영역(로컬 미리보기)"
        style={{
          ...mergedStyle,
          minHeight: mergedStyle?.minHeight ?? 120,
          border: "1px dashed #94a3b8",
          borderRadius: 8,
          color: "#0f172a",
          backgroundColor: "rgba(148, 163, 184, 0.16)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: 14,
          fontWeight: 600,
          textTransform: "uppercase",
          letterSpacing: 1,
        }}
      >
        Ad Placeholder
      </div>
    );
  }

  return (
    <ins
      className="adsbygoogle"
      style={mergedStyle}
      data-ad-client={clientId}
      data-ad-slot={adSlot}
      data-ad-format="auto"
      data-full-width-responsive="true"
    />
  );
}

