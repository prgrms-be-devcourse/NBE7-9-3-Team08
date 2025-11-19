// auth 도메인 API
import { http } from './client'
import type { LoginRequest, LoginResponse, SignupRequest, SignupResponse } from '@/types/auth'
import { UnauthorizedError } from '@/lib/errors/custom-errors';

export interface EmailVerificationRequest {
  email: string
}

export interface EmailVerificationResponse {
  message: string
}

export interface VerifyCodeRequest {
  email: string
  code: string
}

export interface VerifyCodeResponse {
  code: string
  message: string
  data: string
}

export interface GetUserRequest {
  email: string
}

export interface GetUserResponse {
  id: number
  email: string
  name: string
  imageUrl: string | null
}

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> =>
    http.post('/login', data),

  signup: (data: SignupRequest): Promise<SignupResponse> =>
    http.post('/user', data),

  requestEmailVerification: (data: EmailVerificationRequest): Promise<EmailVerificationResponse> =>
    http.post('/auth', data),

  verifyEmailCode: (data: VerifyCodeRequest): Promise<VerifyCodeResponse> =>
    http.post('/verify', data),

  logout: (): Promise<void> =>
    http.post('/logout'),

  refreshToken: (): Promise<LoginResponse> =>
    http.post('/refresh'),

  getCurrentUser: async (): Promise<GetUserResponse | null> => {
    try {
      const { userDto } = await http.get<{ userDto: GetUserResponse }>('/user/me')
      return userDto
    } catch (error) {
      if (error instanceof UnauthorizedError) {
        return null
      }
      throw error
    }
  }
}