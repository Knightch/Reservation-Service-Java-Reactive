package com.springBoot.reservationservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}

}

//@RestController
//@RequiredArgsConstructor
//class ReservationController{
//
//	private final ReservationRepository reservationRepository;
//
//	@GetMapping("/reservation")
//	Flux<Reservation> reservationFlux(){
//		return this.reservationRepository.findAll();
//	}
//
//
//}

@Configuration
class WebsocketConfig{

	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping(){
		return new SimpleUrlHandlerMapping(){
			@Override
			public void setUrlMap(Map<String, ?> urlMap) {
				super.setUrlMap(urlMap);
				setOrder(10);
			}
		};
	}

	@Bean
	WebSocketHandlerAdapter webSocketHandlerAdapter(){
		return new WebSocketHandlerAdapter();
	}

	@Bean
	WebSocketHandler webSocketHandler(GreetingsProducer gp){
		return session -> {
			 var response = session
					.receive()
					.map(WebSocketMessage::getPayloadAsText)
					.map(GreetingRequest::new)
					.flatMap(gp::greeting)
					.map(GreetingResponse::getMessage)
					.map(session::textMessage);
			return session.send(response);
		};
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest{
	private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse{
	private String message;
}

@Component
class GreetingsProducer {
	Flux<GreetingResponse> greeting(GreetingRequest greetingRequest){
		return Flux
				.fromStream(Stream.generate(() -> new GreetingResponse("hello" + greetingRequest.getName() + " @ " + Instant.now() + "!")))
				.delayElements(Duration.ofSeconds(1));
	}
}



@Configuration
class HttpConfiguration {

	@Bean
	RouterFunction<ServerResponse> routes(ReservationRepository rr){
		return route()
				.GET("/reservation", request -> ServerResponse.ok().body(rr.findAll(), Reservation.class))
				.build();
	}
}

@RequiredArgsConstructor
@Component
@Log4j2
class SampleDataInitializer{

	private final ReservationRepository reservationRepository;

	@EventListener(ApplicationReadyEvent.class)
	public void go(){
//		Flux<String> names = Flux.just("Shailey", "Sonam", "Dipti", "Renu", "Aradhya", "Alka", "Nidhi", "Nandani");
//		Flux<Reservation> reservations = names.map(name -> new Reservation(null, name));
//		Flux<Reservation> saved = reservations.flatMap(this.reservationRepository::save);

		var names = Flux
				.just("Java", "JavaScript", ".Net", "Python", "C++", "C", "C#", "Kotlin")
				.map(name -> new Reservation(null, name))
				.flatMap(this.reservationRepository::save);

		this.reservationRepository
				.deleteAll()
				.thenMany(names)
				.thenMany(this.reservationRepository.findAll())
				.subscribe(log::info);
	}

}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, String> {}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class Reservation{

	@Id
	private String id;
	private String name;

}
