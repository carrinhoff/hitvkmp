package pt.hitv.core.domain.usecases

import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.model.Channel

class ToggleFavoriteChannelUseCase(
    private val streamRepository: StreamRepository
) {
    suspend operator fun invoke(channel: Channel) {
        streamRepository.saveFavoriteChannel(channel)
    }
}
