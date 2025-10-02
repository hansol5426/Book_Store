package it.exam.book_purple.filter;

import java.io.IOException;

import org.json.JSONObject;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import it.exam.book_purple.common.utils.JWTUtils;
import it.exam.book_purple.security.dto.UserSecureDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 이전에 JWT를 검증하기 위한 필터
 * 검증에 문제가 없다면 인증정보를 SecurityContextHolder 에 저장
 */
@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter{

    private final JWTUtils jwtUtils;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String accessToken = request.getHeader("Authorization");

        if(accessToken == null){
            log.info("accessToken is null");
            filterChain.doFilter(request, response);
            return;
        }

        try{
            // 보통 토큰 앞에는 "Bearer "이 붙음
            if(accessToken.startsWith("Bearer ")){
                accessToken = accessToken.substring(7);
                
                // 토큰이 만료됐거나 변조됐는지 검사 
                if(!jwtUtils.validateToken(accessToken)){
                    throw new IllegalAccessError("유효하지 않은 토큰입니다.");
                }
            }

            String category = jwtUtils.getCategory(accessToken);

            if(!category.equals("access")){
                throw new IllegalAccessError("유효하지 않은 토큰입니다.");
            }
            
        }catch(Exception e){

            // response 한다
            response.setContentType("application/json");
            JSONObject message = this.getErrorMessage(e.getMessage(),HttpServletResponse.SC_NOT_ACCEPTABLE);
            response.getWriter().write(message.toString());
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);

            return;  // 함수 종료

        }

        // 인증 성공
        String userId = jwtUtils.getUserId(accessToken);
        String userName = jwtUtils.getUserName(accessToken);
        String userRole = jwtUtils.getUserRole(accessToken);

        // UserSecureDTO(String userId, String userName, String passwd, String userRole)
        UserSecureDTO dto = new UserSecureDTO(userId, userName, "", userRole);

        // 시큐리티 세션에 저장
        Authentication authentication = new UsernamePasswordAuthenticationToken(dto, null, dto.getAuthorities());
        // 인증 성공 처리, 스프링 시큐리티에 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 다음으로 이동
        filterChain.doFilter(request, response);

    }

    private JSONObject getErrorMessage(String message, int status){

            JSONObject jObj = new JSONObject();
            jObj.put("resultMsg", message == null ? "Invalid Token" : message );
            jObj.put("status", status);

            return jObj;

    }

}
