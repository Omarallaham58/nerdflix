package com.example.nerdflix;

import com.example.nerdflix.entity.Video;
import com.example.nerdflix.service.VideoProcessingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableAsync
public class NerdflixApplication {

	public static void main(String[] args) {
		SpringApplication.run(NerdflixApplication.class, args);
	}

}
