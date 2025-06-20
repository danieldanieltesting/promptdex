import React from 'react';
import { Link } from 'react-router-dom';

const StarRating = ({ rating }) => {
  const numericRating = Number(rating) || 0;
  const fullStars = Math.round(numericRating);
  const emptyStars = 5 - fullStars;

  return (
    <div className="flex items-center">
      {Array.from({ length: fullStars }, (_, i) => (
        <span key={`star-full-${i}`} className="text-yellow-500">★</span>
      ))}
      {Array.from({ length: emptyStars }, (_, i) => (
        <span key={`star-empty-${i}`} className="text-gray-300">☆</span>
      ))}
    </div>
  );
};

const PromptCard = ({ prompt }) => {
  return (
    <div className="flex flex-col p-6 bg-white rounded-lg border border-gray-200 shadow-md hover:shadow-lg transition-shadow duration-300 h-full">
      <div className="flex-grow">
        {/* The main content area links to the prompt's detail page */}
        <Link to={`/prompts/${prompt.id}`}>
          <h5 className="mb-2 text-2xl font-bold tracking-tight text-gray-900 break-words hover:text-blue-700 transition-colors">
            {prompt.title}
          </h5>
        </Link>
        <p className="font-normal text-gray-700 mt-2 line-clamp-3">{prompt.description}</p>
      </div>

      {/* The footer contains metadata, including a link to the user's profile */}
      <div className="mt-4 pt-4 border-t border-gray-200 flex justify-between items-center">
        <p className="text-sm text-gray-500">
          By: <Link to={`/profile/${prompt.authorUsername}`} className="font-medium text-blue-600 hover:underline">{prompt.authorUsername}</Link>
        </p>
        <StarRating rating={prompt.averageRating} />
      </div>
    </div>
  );
};

export default PromptCard;