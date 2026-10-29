package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import java.util.Optional;

public interface CurrentUserProvider {

    Optional<AuthenticatedUser> currentUser();
}
