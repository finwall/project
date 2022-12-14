package com.hodophilia.SEbackend.controllers;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hodophilia.SEbackend.models.Provider;
import com.hodophilia.SEbackend.models.User;
import com.hodophilia.SEbackend.payload.request.LoginRequest;
import com.hodophilia.SEbackend.payload.request.SignupRequest;
import com.hodophilia.SEbackend.payload.response.JwtResponse;
import com.hodophilia.SEbackend.payload.response.MessageResponse;
import com.hodophilia.SEbackend.repository.UserRepository;
import com.hodophilia.SEbackend.security.jwt.JwtUtils;
import com.hodophilia.SEbackend.security.services.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/login")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, Errors errors) {

		if (errors.hasErrors()) {
			String returnString = "";
			for (ObjectError er : errors.getAllErrors()) {
				returnString += er.getDefaultMessage() + '\n';
			}
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse(returnString));
		}

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    
    return ResponseEntity.ok(new JwtResponse(jwt, 
												 userDetails.getId(), 
												 userDetails.getUsername()
												 ));

		
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest, Errors errors) {

		boolean hasError = 
			userRepository.existsByUsername(signUpRequest.getUsername()) ||
			errors.hasErrors();

		if (hasError) {
			String returnString = "";

			if (userRepository.existsByUsername(signUpRequest.getUsername())) {
				returnString += "Username is already taken.\n";
			}
	

			if (errors.hasErrors()) {
				for (ObjectError er : errors.getAllErrors()) {
					returnString += er.getDefaultMessage() + '\n';
				}
			}
			
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse( returnString ));
		}

		// Create new user's account
		User user = new User(signUpRequest.getUsername(), 
					
							 encoder.encode(signUpRequest.getPassword()), signUpRequest.getFName(), signUpRequest.getLName(),Provider.LOCAL);

		
		
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

}
