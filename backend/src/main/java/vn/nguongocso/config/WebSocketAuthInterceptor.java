package vn.nguongocso.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import vn.nguongocso.auth.service.CustomUserDetailsService;

import java.util.List;

/**
 * Interceptor for validating JWT token on STOMP WebSocket connection.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;

            List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
            if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
                String bearerToken = authorizationHeaders.get(0);
                if (bearerToken.startsWith("Bearer ")) {
                    token = bearerToken.substring(7);
                }
            }

            if (token != null && tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                String orgCode = tokenProvider.getOrganizationCodeFromToken(token);

                UserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(username, orgCode);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                accessor.setUser(authentication);
            }
        }
        return message;
    }
}
