'use client';

import { useEffect, useMemo, useState } from 'react';
import { useToast } from '@/components/ui/Toast';
import { authApi, type GetUserResponse } from '@/lib/api/auth';

const PUBLIC_AUTH_PATTERNS = [
  /^\/$/,
  /^\/login$/,
  /^\/signup$/,
  /^\/email-verification$/,
  /^\/community(?:\/.*)?$/,
  /^\/analysis\/\d+(?:\/.*)?$/,
];

const isPublicPath = (pathname: string) =>
  PUBLIC_AUTH_PATTERNS.some((pattern) => pattern.test(pathname));

export function useAuth() {
  const toast = useToast();
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<GetUserResponse | null>(null);
  const [isLoadingUser, setIsLoadingUser] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true); 
  const [refreshTrigger, setRefreshTrigger] = useState(0);


  const fetchUserInfo = async () => {
    try {
      setIsLoadingUser(true);
      const userData = await authApi.getCurrentUser();
  
      if (!userData) {
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
        return;
      }
      
      setUser(userData);
      setToken('session');
      localStorage.setItem('user', JSON.stringify(userData));
    } catch (error) {
      console.error('사용자 정보 가져오기 실패:', error);
      localStorage.removeItem('user');
      setToken(null);
      setUser(null);
    } finally {
      setIsLoadingUser(false);
      setIsInitializing(false);
    }
  };
  

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const pathname = window.location.pathname;
    const savedUser = localStorage.getItem('user');

    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
        setToken('session');
      } catch {
        localStorage.removeItem('user');
      } finally {
        setIsInitializing(false);
      }
      return;
    }

    if (isPublicPath(pathname)) {
      setIsInitializing(false);
      return;
    }

    fetchUserInfo();
  }, []);

  useEffect(() => {
    if (!token) return;

    const logoutTimer = setTimeout(() => {
      localStorage.removeItem('user');
      setToken(null);
      setUser(null);
      toast.push('세션이 만료되어 자동 로그아웃되었습니다.');
      window.location.href = '/';
    }, 2 * 60 * 60 * 1000);

    return () => clearTimeout(logoutTimer);
  }, [token, toast]);

  const isAuthed = useMemo(() => !!token && !!user, [token, user]);
  
  function loginWithToken(userData: GetUserResponse) {
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setToken('session');
    toast.push('로그인되었습니다.');
  }

  async function logout() {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('서버 로그아웃 실패:', error);
      toast.push('로그아웃 중 오류가 발생했습니다.');
    } finally {
      localStorage.removeItem('user');
      setToken(null);
      setUser(null);
      toast.push('로그아웃되었습니다.');
      window.location.href = '/';
    }
  }

  function updateUserInfo(updatedUser: GetUserResponse) {
    setUser(() => {
      localStorage.setItem('user', JSON.stringify(updatedUser));
      return updatedUser;
    });
    
    setRefreshTrigger(prev => prev + 1);
  }

  return { 
    isAuthed, 
    token, 
    user, 
    isLoadingUser,
    isInitializing,
    refreshTrigger,
    loginWithToken, 
    logout,
    fetchUserInfo,
    updateUserInfo
  };
}
