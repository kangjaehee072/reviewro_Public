package com.reviewro.service;


import com.reviewro.config.ImageFiles;
import com.reviewro.entity.PostImageEntity;
import com.reviewro.entity.TargetType;
import com.reviewro.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostImageService {

    private final PostImageRepository postImageRepository;

    @Value("${file.post.dir}")
    private String postDirText;

    // 글 하나에 사진 여러 장. 저장하고 DB 에 경로를 남긴다
    @Transactional
    public void save(TargetType type, Long targetId, List<MultipartFile> files) {
        if (files == null) {
            return;
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            // 이미지가 아니면 거르고, 저장 이름은 새로 짓는다
            String name = ImageFiles.newName(file);

            try {
                Path dir = Paths.get(postDirText, type.name(), String.valueOf(targetId));
                Files.createDirectories(dir);
                Path target = dir.resolve(name);
                Files.copy(file.getInputStream(), target);

                PostImageEntity image = new PostImageEntity();
                image.setTargetType(type);
                image.setTargetId(targetId);
                image.setImagePath("/data/post/" + type.name() + "/"
                        + targetId + "/" + target.getFileName());
                postImageRepository.save(image);

            } catch (IOException e) {
                throw new UncheckedIOException("사진 저장에 실패했습니다.", e);
            }
        }
    }

    // 글 하나의 사진 목록
    public List<String> paths(TargetType type, Long targetId) {
        List<String> out = new ArrayList<>();
        for (PostImageEntity e : postImageRepository
                .findByTargetTypeAndTargetIdOrderByPostImageIdAsc(type, targetId)) {
            out.add(e.getImagePath());
        }
        return out;
    }

    // 글이 지워질 때. DB 행과 실제 파일을 같이 지운다
    @Transactional
    public void deleteAll(TargetType type, Long targetId) {
        for (PostImageEntity e : postImageRepository
                .findByTargetTypeAndTargetIdOrderByPostImageIdAsc(type, targetId)) {
            try {
                // 경로가 /data/... 로 시작하니 앞을 떼고 실제 폴더에 붙인다
                Path real = Paths.get(postDirText, type.name(),
                        String.valueOf(targetId),
                        Paths.get(e.getImagePath()).getFileName().toString());
                Files.deleteIfExists(real);
            } catch (IOException ignored) {
                // 파일이 이미 없어도 DB 는 지운다
            }
        }
        postImageRepository.deleteByTarget(type, targetId);
    }


}
