package controle.api.back_end.update;

public record UpdateInfo(
        boolean hasUpdate,
        String currentVersion,
        String latestVersion,
        String releaseUrl,
        String downloadUrl
) {}

