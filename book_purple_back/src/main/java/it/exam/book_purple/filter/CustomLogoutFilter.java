package it.exam.book_purple.filter;

import java.io.IOException;
import java.util.Arrays;

import org.json.JSONObject;
import org.springframework.web.filter.GenericFilterBean;

import it.exam.book_purple.common.utils.JWTUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/*
 * 로그아웃 요청이 들어왔을 때
 * 쿠키에 있는 리프레시 토큰을 없애는 것
 */

@RequiredArgsConstructor
public class CustomLogoutFilter extends GenericFilterBean{

    private final JWTUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        process((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void process(HttpServletRequest request,
                         HttpServletResponse response, 
                         FilterChain chain) throws IOException, ServletException{
    
        // 요청한 경로 가져오기
        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();

        // 경로에 로그아웃이 없다면
        if(!requestURI.contains("logout") ||
                !requestMethod.equalsIgnoreCase("POST")){
            // 다음 필터로 넘어감
            chain.doFilter(request, response);
            return;
        }
        
        String refreshToken = "";
        Cookie[] cookies = request.getCookies();
        
        response.setContentType("applicaion/json");

        try{
            
            if(cookies == null){
                throw new IllegalAccessException("쿠키에 정보 없음");
            }

            // 쿠키에 있는 refreshToken 꺼내려고 돌린 거
            refreshToken = Arrays.stream(cookies)
                            .filter(cookie -> cookie.getName().equals("refresh"))
                            .map(Cookie::getValue)
                            .findAny().orElseThrow(()->new IllegalAccessException("없음"));

            // 토큰이 만료됐는지 확인
            if(jwtUtils.getExpired(refreshToken)){
                throw new IllegalAccessException("refresh token 유효기간 지남");
            }

            String category = jwtUtils.getCategory(refreshToken);
            if(!category.equals("refresh")){
                throw new IllegalAccessException("맞지 않는 키입니다.");
            }

            // 로그아웃 처리 : 쿠키 삭제
            Cookie cookie = new Cookie("refresh", null);
            cookie.setMaxAge(0);  // 쿠키 삭제
            cookie.setPath("/");
            response.addCookie(cookie);

            // 성공 응답 보내기 
            response.setStatus(HttpServletResponse.SC_OK);

            JSONObject obj = new JSONObject();

            obj.put("resultMsg", "200");
            obj.put("status", HttpServletResponse.SC_OK);

            response.getWriter().write(obj.toString());
            
            
        }catch(Exception e){

            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            
            JSONObject obj = new JSONObject();
            
            obj.put("resultMsg", "FAIL");
            obj.put("status", HttpServletResponse.SC_BAD_REQUEST);
            
            response.getWriter().write(obj.toString());

        }
    }
    
}
