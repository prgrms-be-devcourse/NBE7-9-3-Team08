package com.backend.domain.user.controller

import com.backend.domain.user.dto.UserDto
import com.backend.domain.user.entity.User
import com.backend.domain.user.service.JwtService
import com.backend.domain.user.service.UserService
import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import com.backend.global.response.ApiResponse
import jakarta.mail.MessagingException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    private val userService: UserService,
    private val jwtService: JwtService,
    private val jwtUtil: JwtUtil
) {
    // id를 입력받아 회원이 존재하면 해당 회원을 반환하는 api
    @JvmRecord
    data class GetRequest(
        @field:NotBlank(message = "이메일은 필수 입력값 입니다.")
        @field:Email(message = "이메일 형식이 아닙니다.")
        val email : String
    )

    @JvmRecord
    data class GetResponse(
        val userDto: UserDto
    )

    @GetMapping("/api/user")
    fun getUser(
        @Valid @RequestBody request: GetRequest
    ): ApiResponse<GetResponse> {
        val user = userService.findByEmail(request.email)
        return ApiResponse.success(GetResponse(UserDto(user)))
    }


    //모든 회원을 조회하는 api
    @JvmRecord
    data class GetUsersResponse(
        val userDtoList: List<UserDto>
    )


    @get:GetMapping("/api/users")
    val users: ApiResponse<GetUsersResponse>
        get() {
            println("다건 조회")
            val users: List<User> = userService.findByAll()

            val userDtoList = users.map{ UserDto(it) }

            return ApiResponse.success(GetUsersResponse(userDtoList))
        }


    //email, password, passwrodCheck, name을 입력받아 회원가입을 진행하는 api
    @JvmRecord
    data class JoinRequest(
        @field:NotBlank(message = "이메일은 필수 입력값 입니다.")
        @field:Email(message = "이메일 형식이 아닙니다.")
        val email: String,

        @field:NotBlank(message = "비밀번호는 필수 입력값 입니다.")
        val password: String,

        @field:NotBlank(message = "비밀번호 확인은 필수 입력값 입니다.")
        val passwordCheck:  String,

        @field:NotBlank(message = "사용자 이름은 필수 입력값 입니다.")
        val name: String
    )
//여기부터
    @JvmRecord
    data class JoinResponse(
        val userDto: UserDto
    )

    @PostMapping("/api/user")
    @Throws(MessagingException::class)
    fun join(
        @Valid @RequestBody joinRequest: JoinRequest
    ): ApiResponse<JoinResponse> {
        val user = userService.join(
            joinRequest.email,
            joinRequest.password,
            joinRequest.passwordCheck,
            joinRequest.name
        )

        return ApiResponse.success(JoinResponse(UserDto(user)))
    }

    //soft delete
    @JvmRecord
    data class DeleteRequest(
        @field:NotBlank(message = "이메일은 필수 입력값 입니다.")
        @field:Email(message = "이메일 형식이 아닙니다.")
        val email: String
    )

    @JvmRecord
    data class DeleteResponse(
        val userDto: UserDto
    )

    @DeleteMapping("/api/user")
    fun softDelete(
        @Valid @RequestBody deleteRequest: DeleteRequest
    ): ApiResponse<DeleteResponse> {
        val softDeleteUser: User = userService.softdelete(deleteRequest.email)
        return ApiResponse.success(DeleteResponse(UserDto(softDeleteUser)))
    }


    //이름 변경
    @JvmRecord
    data class ModifyNameRequest(
        @field:NotBlank(message = "이름은 필수 입력값 입니다.")
        val name: String
    )

    @JvmRecord
    data class ModifyNameResponse(
        val userDto: UserDto
    )

    @PostMapping("/api/user/name")
    fun modifyName(
        request: HttpServletRequest,
        @Valid @RequestBody modifyNameRequest: ModifyNameRequest
    ): ApiResponse<ModifyNameResponse> {
        val email : String? = jwtUtil.getEmail(request)

        if(email.isNullOrBlank()){
            throw BusinessException(ErrorCode.EMAIL_NOT_FOUND)
        }

        println("email: ${email}")
        println("변경전 name : ${modifyNameRequest.name}")
        val user = userService.modifyName(email, modifyNameRequest.name)

        println("변경후 name : ${user.name}")
        return ApiResponse.success(ModifyNameResponse(UserDto(user)))
    }

    //비밀번호 변경
    @JvmRecord
    data class ModifyPasswordRequest(
        @field:NotBlank(message = "비밀번호는 필수 입력값 입니다.")
        val password: String,
        @field:NotBlank(message = "비밀번호는 필수 입력값 입니다.")
        val passwordCheck: String
    )

    @JvmRecord
    data class ModifyPasswordResponse(
        val userDto: UserDto
    )

    @PostMapping("/api/user/password")
    fun modifyPassword(
        request: HttpServletRequest,
        @Valid  @RequestBody modifyPasswordRequest: ModifyPasswordRequest
    ): ApiResponse<ModifyPasswordResponse> {
        val email = jwtUtil.getEmail(request)
        if(email.isNullOrBlank()){
            throw BusinessException(ErrorCode.EMAIL_NOT_FOUND)
        }

        val user = userService.modifyPassword(
            email,
            modifyPasswordRequest.password,
            modifyPasswordRequest.passwordCheck
        )
        return ApiResponse.success(ModifyPasswordResponse(UserDto(user)))
    }

    //삭제된 유저 복구
    @JvmRecord
    data class RestoreRequest(
        @field:NotBlank(message = "이메일은 필수 입력값 입니다.")
        @field:Email(message = "이메일 형식이 아닙니다.")
        val email: String
    )

    @JvmRecord
    data class RestoreResponse(
        val userDto: UserDto
    )  /*
    @PostMapping("/api/user/restore")
    public ApiResponse<RestoreResponse> restoreUser(
            @Valid @RequestBody RestoreRequest restoreRequest
    ){
        System.out.println("restore aip 호출");
        User user = userService.restore(restoreRequest.email);
        return ApiResponse.success(new RestoreResponse(new UserDto(user)));
    }
*/
}