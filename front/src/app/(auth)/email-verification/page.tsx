// 회원가입 이메일 인증 페이지
'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/lib/api/auth';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/Button';
import { Loader2, CheckCircle2, AlertTriangle } from 'lucide-react';

type Status = 'pending' | 'success' | 'error';

export default function EmailVerificationPage() {
  const router = useRouter();
  const params = useSearchParams();
  const email = params.get('email');
  const code = params.get('code');
  const [status, setStatus] = useState<Status>('pending');
  const [message, setMessage] = useState('인증을 확인하고 있습니다...');

  useEffect(() => {
    if (!email || !code) {
      setStatus('error');
      setMessage('잘못된 인증 링크입니다. 다시 회원가입을 진행해 주세요.');
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        await authApi.verifyEmailCode({ email, code });
        if (cancelled) return;
        setStatus('success');
        setMessage('인증이 완료되었습니다. 잠시 후 로그인 화면으로 이동합니다.');
        setTimeout(() => router.push('/login'), 2000);
      } catch (err: any) {
        if (cancelled) return;
        setStatus('error');
        setMessage(err?.message ?? '인증에 실패했습니다. 다시 시도해 주세요.');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [email, code, router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <Card className="w-full max-w-md text-center">
        <CardHeader>
          <CardTitle>이메일 인증</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {status === 'pending' && (
            <>
              <Loader2 className="mx-auto h-12 w-12 animate-spin text-primary" />
              <p className="text-muted-foreground">{message}</p>
            </>
          )}
          {status === 'success' && (
            <>
              <CheckCircle2 className="mx-auto h-12 w-12 text-green-500" />
              <p>{message}</p>
              <Button className="w-full" onClick={() => router.push('/login')}>
                로그인으로 이동
              </Button>
            </>
          )}
          {status === 'error' && (
            <>
              <AlertTriangle className="mx-auto h-12 w-12 text-destructive" />
              <p className="text-destructive">{message}</p>
              <Button className="w-full" variant="outline" onClick={() => router.push('/signup')}>
                회원가입 다시 하기
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
