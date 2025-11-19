"use client";

import { useEffect, useMemo, useState, useRef } from "react";
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
  const [adLoaded, setAdLoaded] = useState(false);
  const insRef = useRef<HTMLModElement | null>(null);

  useEffect(() => {
    if (process.env.NODE_ENV === "production") {
      const hostname =
        typeof window !== "undefined" ? window.location.hostname : "";
      if (hostname === "localhost" || hostname === "127.0.0.1") {
        setForcePreview(true);
      }
    }
  }, []);

  useEffect(() => {
    setAdLoaded(false);
  }, [adSlot, clientId, forcePreview]);

  const shouldRenderAd = !forcePreview && !!clientId;

  const showFallback = useMemo(() => {
    if (!shouldRenderAd) return true;
    return !adLoaded;
  }, [shouldRenderAd, adLoaded]);

  useEffect(() => {
    if (!shouldRenderAd) return;

    try {
      if (typeof window !== "undefined") {
        (window.adsbygoogle = window.adsbygoogle || []).push({});
      }
    } catch (e) {
      console.error("Adsense error", e);
    }
  }, [shouldRenderAd, adSlot]);

  useEffect(() => {
    if (!shouldRenderAd) return;
    const target = insRef.current;
    if (!target) return;

    const observer = new MutationObserver(() => {
      if (target.childElementCount > 0) {
        setAdLoaded(true);
        observer.disconnect();
      }
    });

    observer.observe(target, { childList: true });
    return () => observer.disconnect();
  }, [shouldRenderAd, adSlot]);

  const containerStyle: CSSProperties = {
    ...mergedStyle,
    border: "1px solid rgba(15, 23, 42, 0.1)",
    borderRadius: mergedStyle.borderRadius ?? 12,
    backgroundColor: "#fff",
    position: "relative",
    overflow: "hidden",
  };

  return (
    <div style={containerStyle}>
      <a
        href="https://programmers.co.kr/"
        target="_blank"
        rel="noopener noreferrer"
        style={{
          position: "absolute",
          inset: 0,
          display: showFallback ? "flex" : "none",
          alignItems: "center",
          justifyContent: "center",
          padding: 16,
          background: "#fff",
          zIndex: 2,
        }}
        aria-label="프로그래머스 프로모션"
      >
        <img
          src="/programmers.png"
          alt="프로그래머스 광고"
          style={{
            maxWidth: "100%",
            maxHeight: "100%",
            objectFit: "contain",
          }}
        />
      </a>
      {shouldRenderAd && (
        <ins
          ref={insRef}
          className="adsbygoogle"
          style={{
            display: "block",
            width: "100%",
            height: "100%",
            minHeight: mergedStyle.minHeight ?? 120,
          }}
          data-ad-client={clientId}
          data-ad-slot={adSlot}
          data-ad-format="auto"
          data-full-width-responsive="true"
        />
      )}
    </div>
  );
}

