package in.chinche.hospital.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/** Uniform pagination envelope for all list endpoints. */
public record PageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageDto<T> of(Page<T> p) {
        return new PageDto<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    public static <S, T> PageDto<T> of(Page<S> p, java.util.function.Function<S, T> mapper) {
        return new PageDto<>(p.getContent().stream().map(mapper).toList(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }
}
